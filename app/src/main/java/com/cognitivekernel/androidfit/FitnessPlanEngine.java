package com.cognitivekernel.androidfit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Offline evidence-informed planning calculations.
 * Nutrition values are estimates from generic cooked/raw food composition values,
 * not a substitute for a clinician or a branded-product label.
 */
public final class FitnessPlanEngine {
    private FitnessPlanEngine() {}

    public static final class Profile {
        public int age = 30;
        public double heightCm = 170;
        public double weightKg = 70;
        public int sexIndex;
        public int goalIndex = 1; // 0 strength, 1 hypertrophy, 2 general fitness, 3 fat loss
        public int activityIndex = 1;
        public int levelIndex;
        public int equipmentIndex;
        public int trainingDays = 3;
        public int sessionMinutes = 45;
        public int mealsPerDay = 4;
        public String conditions = "";
        public String injuries = "";
        public String medications = "";
        public String allergies = "";
        public String dietaryPreference = "omnivore";
        public String dislikedFoods = "";
        public String notes = "";
    }

    public static final class Analysis {
        public final double bmi, bmr, maintenanceCalories, targetCalories;
        public final double proteinGrams, fatGrams, carbohydrateGrams;
        public final boolean needsMedicalReview;
        public final String medicalReason;

        Analysis(double bmi, double bmr, double maintenanceCalories, double targetCalories,
                 double proteinGrams, double fatGrams, double carbohydrateGrams,
                 boolean needsMedicalReview, String medicalReason) {
            this.bmi = bmi;
            this.bmr = bmr;
            this.maintenanceCalories = maintenanceCalories;
            this.targetCalories = targetCalories;
            this.proteinGrams = proteinGrams;
            this.fatGrams = fatGrams;
            this.carbohydrateGrams = carbohydrateGrams;
            this.needsMedicalReview = needsMedicalReview;
            this.medicalReason = medicalReason;
        }
    }

    private static final class Food {
        final String name;
        final double kcal100, protein100, carbs100, fat100, unitGrams;
        final String unit;
        final int group; // 0 protein, 1 carbohydrate/produce, 2 fat
        Food(String name, double kcal100, double protein100, double carbs100,
             double fat100, double unitGrams, String unit, int group) {
            this.name=name; this.kcal100=kcal100; this.protein100=protein100;
            this.carbs100=carbs100; this.fat100=fat100; this.unitGrams=unitGrams;
            this.unit=unit; this.group=group;
        }
    }

    private static final class Portion {
        final Food food;
        final double grams;
        Portion(Food food, double grams) { this.food=food; this.grams=grams; }
        double kcal() { return food.kcal100*grams/100.0; }
        double protein() { return food.protein100*grams/100.0; }
        double carbs() { return food.carbs100*grams/100.0; }
        double fat() { return food.fat100*grams/100.0; }
    }

    private static final class Meal {
        final String title;
        final List<Portion> portions = new ArrayList<>();
        Meal(String title) { this.title=title; }
        void add(Food food,double grams) { portions.add(new Portion(food,grams)); }
    }

    public static Analysis analyze(Profile p) {
        validate(p);
        double bmi=p.weightKg/Math.pow(p.heightCm/100.0,2);
        double bmr=10*p.weightKg+6.25*p.heightCm-5*p.age+(p.sexIndex==0?5:-161);
        double[] activity={1.20,1.375,1.55,1.725};
        double maintenance=bmr*activity[p.activityIndex];
        double target=maintenance;
        if(p.goalIndex==3 && bmi>=18.5) target=maintenance-Math.min(500,maintenance*0.15);
        else if(p.goalIndex==1) target=maintenance+Math.min(250,maintenance*0.08);
        else if(p.goalIndex==0) target=maintenance+Math.min(150,maintenance*0.05);
        target=Math.max(1200,target);
        if(p.goalIndex==3 && bmi<18.5) target=maintenance;
        double proteinRate=p.goalIndex==3?1.6:(p.goalIndex==1||p.goalIndex==0?1.8:1.4);
        double protein=Math.min(2.2*p.weightKg,proteinRate*p.weightKg);
        double fat=Math.max(0.6*p.weightKg,0.8*p.weightKg);
        double carbs=Math.max(0,(target-protein*4-fat*9)/4);
        String reason=medicalReviewReason(p);
        return new Analysis(bmi,bmr,maintenance,target,protein,fat,carbs,!reason.isEmpty(),reason);
    }

    public static void validate(Profile p) {
        if(p==null) throw new IllegalArgumentException("Profile is required");
        if(p.age<18||p.age>100||p.heightCm<120||p.heightCm>230||p.weightKg<30||p.weightKg>300
                ||p.sexIndex<0||p.sexIndex>1||p.goalIndex<0||p.goalIndex>3
                ||p.activityIndex<0||p.activityIndex>3||p.levelIndex<0||p.levelIndex>2
                ||p.equipmentIndex<0||p.equipmentIndex>2||p.trainingDays<1||p.trainingDays>7
                ||p.sessionMinutes<10||p.sessionMinutes>180||p.mealsPerDay<3||p.mealsPerDay>6) {
            throw new IllegalArgumentException("Profile values are outside supported ranges");
        }
    }

    private static String medicalReviewReason(Profile p) {
        StringBuilder result=new StringBuilder();
        if(!blank(p.conditions)) result.append("Reported medical conditions: ").append(p.conditions.trim()).append(". ");
        if(!blank(p.injuries)) result.append("Reported pain/injury/limitations: ").append(p.injuries.trim()).append(". ");
        if(!blank(p.medications)) result.append("Medication use reported: ").append(p.medications.trim()).append(". ");
        if(!blank(result.toString())) result.append("Get individualized clearance from a qualified clinician before starting or changing exercise/nutrition; this offline app cannot assess severity or medication interactions.");
        return result.toString();
    }

    public static String trainingReport(Profile p, Analysis a, List<PersonalPlanGenerator.PlannedExercise> exercises) {
        validate(p);
        StringBuilder out=new StringBuilder();
        out.append("PROFILE & TARGET\n")
           .append("Age: ").append(p.age).append(" | Height: ").append(round(p.heightCm)).append(" cm | Weight: ").append(round(p.weightKg)).append(" kg\n")
           .append("BMI screening estimate: ").append(round(a.bmi)).append(" (not a diagnosis)\n")
           .append("Estimated maintenance: ").append(Math.round(a.maintenanceCalories)).append(" kcal/day\n")
           .append("Starting energy target: ").append(Math.round(a.targetCalories)).append(" kcal/day\n\n")
           .append("WEEKLY TRAINING STRUCTURE\n");
        int days=p.trainingDays;
        for(int day=1;day<=days;day++) {
            out.append("Day ").append(day).append(": ");
            if(days<=2) out.append("Full body");
            else if(days==3) out.append(day==1?"Full body A":day==2?"Full body B":"Full body C");
            else if(days==4) out.append(day%2==1?"Upper body":"Lower body");
            else if(days==5) out.append(day==1||day==4?"Push":day==2||day==5?"Pull":"Legs & core");
            else out.append(day%2==1?"Upper body":"Lower body");
            out.append(" — ").append(p.sessionMinutes).append(" min\n");
        }
        out.append("\nSESSION PRESCRIPTION\n");
        int sets=p.goalIndex==1?3:p.goalIndex==0?3:2;
        int repsLow=p.goalIndex==0?4:p.goalIndex==1?6:8;
        int repsHigh=p.goalIndex==0?8:p.goalIndex==1?12:15;
        int weeklySets=p.levelIndex==0?6:p.levelIndex==1?10:12;
        out.append("Use ").append(sets).append(" working sets per selected movement, ").append(repsLow).append("–").append(repsHigh)
           .append(" reps, controlled full range of motion, and 2–3 minutes rest for compound lifts (about 60–120 sec for smaller isolation work).\n")
           .append("Initial volume: approximately ").append(weeklySets).append(" challenging sets per major muscle group per week, adjusted to recovery and experience. For hypertrophy, build toward ~10+ weekly sets only when tolerated.\n")
           .append("Effort: finish most sets with about 2–3 good repetitions in reserve; do not force failure. Add repetitions first, then the smallest available load when all sets reach the top of the range with sound technique.\n")
           .append("Warm up 5–10 minutes and perform lighter ramp-up sets before demanding lifts. Stop for chest pain, faintness, unusual breathlessness, or sharp/worsening pain.\n");
        if(exercises!=null&&!exercises.isEmpty()) {
            out.append("\nMOVEMENT SELECTION FROM THE INSTALLED EXERCISE CATALOG\n");
            int i=1;
            for(PersonalPlanGenerator.PlannedExercise item:exercises) {
                out.append(i++).append(". ").append(item.asset.name).append(" — ")
                   .append(item.sets).append(" sets × ").append(item.reps).append(" reps\n");
            }
        }
        out.append("\nRECOVERY & REVIEW\n")
           .append("Aim for 7–9 hours of sleep where possible. Keep at least one recovery day each week. Review performance, body-weight trend, pain, sleep and session effort every 2 weeks; adjust volume or energy intake in small steps rather than reacting to one weigh-in.\n");
        if(a.needsMedicalReview) out.append("\nMEDICAL SCREENING — REVIEW REQUIRED\n").append(a.medicalReason).append("\n");
        if(a.bmi<18.5) out.append("\nBMI screening is below 18.5; do not use a fat-loss target. Seek a clinician/dietitian assessment.\n");
        if(a.bmi>=30) out.append("\nBMI screening is 30 or above; BMI alone does not determine fitness or diagnosis. Progress gradually and seek clinical guidance if there are symptoms or comorbidities.\n");
        return out.toString();
    }

    public static String nutritionReport(Profile p, Analysis a) {
        validate(p);
        List<Meal> meals=createMeals(p,a);
        StringBuilder out=new StringBuilder();
        out.append("DAILY NUTRITION TARGET (ESTIMATE)\n")
           .append("Energy: ").append(Math.round(a.targetCalories)).append(" kcal\n")
           .append("Protein: ").append(Math.round(a.proteinGrams)).append(" g | Carbohydrate: ").append(Math.round(a.carbohydrateGrams))
           .append(" g | Fat: ").append(Math.round(a.fatGrams)).append(" g\n")
           .append("Values are estimates; brands, cooking method, water loss and edible portion change the actual values. Weigh rice/potato/meat in the stated cooked state.\n\n");
        double totalK=0,totalP=0,totalC=0,totalF=0;
        for(Meal meal:meals) {
            double mk=0,mp=0,mc=0,mf=0;
            out.append(meal.title).append("\n");
            for(Portion portion:meal.portions) {
                if(portion.grams<=0) continue;
                mk+=portion.kcal();mp+=portion.protein();mc+=portion.carbs();mf+=portion.fat();
                String amount;
                if("egg whites".equals(portion.food.unit)) amount=Math.max(1,Math.round(portion.grams/portion.food.unitGrams))+" egg whites (~"+Math.round(portion.grams)+" g)";
                else amount=Math.round(portion.grams)+" g";
                out.append("• ").append(portion.food.name).append(": ").append(amount)
                   .append(" — ").append(Math.round(portion.kcal())).append(" kcal\n");
            }
            totalK+=mk;totalP+=mp;totalC+=mc;totalF+=mf;
            out.append("Meal subtotal: ").append(Math.round(mk)).append(" kcal | P ").append(Math.round(mp))
               .append(" g | C ").append(Math.round(mc)).append(" g | F ").append(Math.round(mf)).append(" g\n\n");
        }
        out.append("DAILY MENU TOTAL: ").append(Math.round(totalK)).append(" kcal | Protein ").append(Math.round(totalP))
           .append(" g | Carbs ").append(Math.round(totalC)).append(" g | Fat ").append(Math.round(totalF)).append(" g\n")
           .append("Adjust portions after 2–3 weeks using average weight trend, training performance, hunger and recovery. Do not treat calorie estimates as exact laboratory values.\n");
        if(a.needsMedicalReview) out.append("\nMEDICAL/DIETARY REVIEW REQUIRED\n").append(a.medicalReason).append("\n");
        if(!blank(p.allergies)) out.append("\nALLERGY NOTE: ").append(p.allergies).append(". The sample menu is generic and must be checked against the stated allergies before use.\n");
        if(!blank(p.dislikedFoods)) out.append("Foods to avoid/replace: ").append(p.dislikedFoods).append("\n");
        return out.toString();
    }

    private static List<Meal> createMeals(Profile p,Analysis a) {
        Food oats=food("Rolled oats (dry)",389,16.9,66.3,6.9,0,"g",1);
        Food egg=food("Egg whites",52,10.9,0.7,0.2,33,"egg whites",0);
        Food banana=food("Banana (edible)",89,1.1,22.8,0.3,0,"g",1);
        Food chicken=food("Chicken breast (cooked)",165,31,0,3.6,0,"g",0);
        Food rice=food("White rice (cooked)",130,2.7,28.2,0.3,0,"g",1);
        Food veg=food("Mixed non-starchy vegetables",35,2,7,0.3,0,"g",1);
        Food oil=food("Olive oil",884,0,0,100,0,"g",2);
        Food yogurt=food("Plain Greek yogurt, low-fat",73,10,3.9,2,0,"g",0);
        Food apple=food("Apple (edible)",52,0.3,13.8,0.2,0,"g",1);
        Food almonds=food("Almonds",579,21.2,21.6,49.9,0,"g",2);
        Food beef=food("Lean beef (cooked)",217,26,0,12,0,"g",0);
        Food potato=food("Potato (boiled)",87,1.9,20.1,0.1,0,"g",1);
        Meal breakfast=new Meal("Breakfast");
        breakfast.add(oats,50);breakfast.add(egg,132);breakfast.add(banana,100);
        Meal lunch=new Meal("Lunch");
        lunch.add(chicken,150);lunch.add(rice,180);lunch.add(veg,150);lunch.add(oil,5);
        Meal snack=new Meal("Snack");
        snack.add(yogurt,200);snack.add(apple,150);snack.add(almonds,10);
        Meal dinner=new Meal("Dinner");
        dinner.add(beef,130);dinner.add(potato,200);dinner.add(veg,150);dinner.add(oil,5);
        List<Meal> result=new ArrayList<>();Collections.addAll(result,breakfast,lunch,snack,dinner);
        double baseProtein=0,baseFat=0,baseCarbs=0;
        for(Meal m:result)for(Portion x:m.portions){baseProtein+=x.protein();baseFat+=x.fat();baseCarbs+=x.carbs();}
        double pFactor=baseProtein<=0?1:clamp(a.proteinGrams/baseProtein,0.65,1.8);
        double fFactor=baseFat<=0?1:clamp(a.fatGrams/baseFat,0.65,1.8);
        for(Meal m:result) {
            for(int i=0;i<m.portions.size();i++) {
                Portion x=m.portions.get(i);
                double factor=x.food.group==0?pFactor:x.food.group==2?fFactor:1.0;
                m.portions.set(i,new Portion(x.food,x.grams*factor));
            }
        }
        double currentK=0,carbK=0;
        for(Meal m:result)for(Portion x:m.portions){currentK+=x.kcal();if(x.food.group==1)carbK+=x.kcal();}
        double carbFactor=carbK<=0?1:clamp((a.targetCalories-currentK+carbK)/carbK,0.5,2.5);
        for(Meal m:result) {
            for(int i=0;i<m.portions.size();i++) {
                Portion x=m.portions.get(i);
                if(x.food.group==1)m.portions.set(i,new Portion(x.food,x.grams*carbFactor));
            }
        }
        if(!"omnivore".equals(p.dietaryPreference)) {
            // Vegetarian/vegan profiles must not silently receive meat or egg portions.
            for(Meal m:result) {
                m.portions.removeIf(x -> "vegetarian".equals(p.dietaryPreference)
                        ? ("Chicken breast (cooked)".equals(x.food.name)||"Lean beef (cooked)".equals(x.food.name))
                        : ("Chicken breast (cooked)".equals(x.food.name)||"Lean beef (cooked)".equals(x.food.name)
                           ||"Egg whites".equals(x.food.name)||"Plain Greek yogurt, low-fat".equals(x.food.name)));
            }
            result.get(1).add(food("Lentils (cooked)",116,9,20.1,0.4,0,"g",0),180*pFactor);
            result.get(3).add(food("Firm tofu",144,17.3,2.8,8.7,0,"g",0),150*pFactor);
            if("vegan".equals(p.dietaryPreference)) {
                result.get(0).portions.removeIf(x -> "Egg whites".equals(x.food.name));
                result.get(2).add(food("Unsweetened soy yogurt",50,4,3,2.5,0,"g",0),200*pFactor);
            }
        }
        return result;
    }

    private static Food food(String name,double kcal,double protein,double carbs,double fat,double unit,String unitName,int group) {
        return new Food(name,kcal,protein,carbs,fat,unit,unitName,group);
    }
    private static boolean blank(String s){return s==null||s.trim().isEmpty();}
    private static double clamp(double v,double min,double max){return Math.max(min,Math.min(max,v));}
    private static double round(double v){return Math.round(v*10.0)/10.0;}
}
