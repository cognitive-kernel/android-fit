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
        public boolean english;
        public boolean medicalClearance;
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
        public double sleepHours = 0;
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
        double bmr=10*p.weightKg+6.25*p.heightCm-5*p.age+(p.sexIndex==0?5:p.sexIndex==1?-161:-78);
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
        if(blank(p.conditions)||blank(p.injuries)||blank(p.medications)||blank(p.allergies)) {
            throw new IllegalArgumentException("Enter none explicitly when no condition, injury, medicine or food allergy applies");
        }
        if(p.age<18||p.age>100||p.heightCm<120||p.heightCm>230||p.weightKg<30||p.weightKg>300
                ||p.sexIndex<0||p.sexIndex>2||p.goalIndex<0||p.goalIndex>3
                ||p.activityIndex<0||p.activityIndex>3||p.levelIndex<0||p.levelIndex>2
                ||p.equipmentIndex<0||p.equipmentIndex>2||p.trainingDays<1||p.trainingDays>6
                ||p.sessionMinutes<10||p.sessionMinutes>180||p.mealsPerDay<3||p.mealsPerDay>5
                ||(!"omnivore".equals(p.dietaryPreference)&&!"vegetarian".equals(p.dietaryPreference)&&!"vegan".equals(p.dietaryPreference))
                ||Double.isNaN(p.heightCm)||Double.isInfinite(p.heightCm)||Double.isNaN(p.weightKg)||Double.isInfinite(p.weightKg)
                ||Double.isNaN(p.sleepHours)||Double.isInfinite(p.sleepHours)||p.sleepHours<0||p.sleepHours>24) {
            throw new IllegalArgumentException("Profile values are outside supported ranges");
        }
    }

    private static String medicalReviewReason(Profile p) {
        StringBuilder result=new StringBuilder();
        if(!blank(p.conditions) && !isNone(p.conditions)) result.append("Reported medical conditions: ").append(p.conditions.trim()).append(". ");
        if(!blank(p.injuries) && !isNone(p.injuries)) result.append("Reported pain/injury/limitations: ").append(p.injuries.trim()).append(". ");
        if(!blank(p.medications) && !isNone(p.medications)) result.append("Medication use reported: ").append(p.medications.trim()).append(". ");
        if(!blank(result.toString())) result.append("Reported health information requires clinician-specific guidance; this offline app cannot assess severity, contraindications or medication interactions.");
        return result.toString();
    }

    public static String trainingReport(Profile p, Analysis a, List<PersonalPlanGenerator.PlannedExercise> exercises) {
        validate(p);
        StringBuilder out=new StringBuilder();
        out.append(tr(p,"PROFILE & TARGET\
","مشخصات و هدف\
"))
           .append(tr(p,"Age: ","سن: ")).append(p.age)
           .append(tr(p," | Height: "," | قد: ")).append(round(p.heightCm))
           .append(tr(p," cm | Weight: "," سانتی‌متر | وزن: ")).append(round(p.weightKg)).append(" kg\
")
           .append(tr(p,"BMI screening estimate (not a diagnosis): ","BMI تخمینی (تشخیص پزشکی نیست): ")).append(round(a.bmi)).append("\
")
           .append(tr(p,"Estimated maintenance: ","کالری نگهدارنده تخمینی: ")).append(Math.round(a.maintenanceCalories)).append(" kcal/day\
")
           .append(tr(p,"Starting energy target: ","هدف اولیه انرژی: ")).append(Math.round(a.targetCalories)).append(" kcal/day\
\
")
           .append(tr(p,"WEEKLY TRAINING STRUCTURE\
","ساختار تمرین هفتگی\
"));
        if(a.needsMedicalReview&&!p.medicalClearance) {
            out.append(tr(p,"\
MEDICAL REVIEW REQUIRED BEFORE TRAINING\
","\
پیش از تجویز تمرین، بررسی پزشکی لازم است\
"))
               .append(a.medicalReason).append("\
")
               .append(tr(p,"No individualized exercise prescription has been generated until a qualified clinician reviews the reported condition, injury or medication.\
",
                   "تا زمان بررسی شرایط پزشکی، آسیب یا دارو توسط متخصص واجد صلاحیت، نسخه تمرینی اختصاصی تولید نمی‌شود.\
"));
            return out.toString();
        }
        for(int day=1;day<=p.trainingDays;day++) {
            String split;
            if(p.trainingDays<=2) split=tr(p,"Full body","کل بدن");
            else if(p.trainingDays==3) split=tr(p,day==1?"Full body A":day==2?"Full body B":"Full body C",day==1?"کل بدن الف":day==2?"کل بدن ب":"کل بدن ج");
            else if(p.trainingDays==4) split=tr(p,day%2==1?"Upper body":"Lower body",day%2==1?"بالاتنه":"پایین‌تنه");
            else if(p.trainingDays==5) split=tr(p,day==1?"Push":day==2?"Pull":day==3?"Legs & core":day==4?"Upper body":"Lower body",day==1?"فشاری":day==2?"کششی":day==3?"پا و میان‌تنه":day==4?"بالاتنه":"پایین‌تنه");
            else split=tr(p,day%2==1?"Upper body":"Lower body",day%2==1?"بالاتنه":"پایین‌تنه");
            out.append(tr(p,"Day ","روز ")).append(day).append(": ").append(split)
               .append(" — ").append(p.sessionMinutes).append(tr(p," min\
"," دقیقه\
"));
        }
        int sets=(p.trainingDays>=5||p.levelIndex==0)?2:3;
        int repsLow=p.goalIndex==0?4:p.goalIndex==1?6:8;
        int repsHigh=p.goalIndex==0?8:p.goalIndex==1?12:15;
        out.append(tr(p,"\
SESSION PRESCRIPTION\
","\
نسخه تمرین هر جلسه\
"))
           .append(tr(p,"Use ","برای هر حرکت ")).append(sets).append(tr(p," working sets, "," ست کاری، "))
           .append(repsLow).append("–").append(repsHigh)
           .append(tr(p," reps. Use controlled full range of motion; rest 2–3 min for compound lifts and 60–120 sec for isolation work.\
",
               " تکرار با دامنه کامل و کنترل‌شده اجرا کنید؛ استراحت حرکات چندمفصلی ۲ تا ۳ دقیقه و حرکات تک‌مفصلی ۶۰ تا ۱۲۰ ثانیه باشد.\
"))
           .append(tr(p,"Initial volume: start with 2 working sets per selected movement. Count weekly sets from the day-by-day assignment and increase gradually only when recovery allows.\
",
               "حجم شروع: برای هر حرکت انتخاب‌شده ۲ ست کاری اجرا کنید. مجموع ست‌های هفتگی را از تقسیم‌بندی روزها محاسبه و فقط در صورت ریکاوری مناسب به‌تدریج افزایش دهید.\
"))
           .append(tr(p,"Effort: keep about 2–3 good repetitions in reserve on most sets; failure is not required. Add reps first, then the smallest load increase when all sets reach the top of the range with good technique.\
",
               "شدت: در پایان بیشتر ست‌ها حدود ۲ تا ۳ تکرار توان باقی بماند؛ تمرین تا ناتوانی ضروری نیست. ابتدا تکرار را افزایش دهید و پس از رسیدن همه ست‌ها به سقف دامنه با تکنیک صحیح، کمترین افزایش وزنه را اعمال کنید.\
"))
           .append(tr(p,"Warm up 5–10 minutes and perform lighter ramp-up sets before demanding lifts. Stop for chest pain, faintness, unusual breathlessness, or sharp/worsening pain.\
",
               "۵ تا ۱۰ دقیقه گرم کنید و پیش از حرکات سنگین چند ست سبک انجام دهید. در صورت درد قفسه سینه، غش، تنگی نفس غیرعادی یا درد تیز و تشدیدشونده تمرین را متوقف کنید.\
"));
        if(exercises!=null&&!exercises.isEmpty()) {
            out.append(tr(p,"\
DAY-BY-DAY EXERCISE ASSIGNMENT\
","\
تقسیم حرکات در روزهای تمرین\
"));
            for(int day=1;day<=p.trainingDays;day++) {
                String split=splitKey(p,day);
                out.append(tr(p,"Day ","روز ")).append(day).append(" — ").append(splitLabel(p,split)).append("\
");
                int shown=0;
                for(PersonalPlanGenerator.PlannedExercise item:exercises) {
                    String category=PersonalPlanGenerator.category(item.asset.name);
                    if(item.day!=day||!matchesSplit(split,category)) continue;
                    appendExercise(out,p,item,++shown);
                }
            }
        }
        out.append(tr(p,"\
RECOVERY & REVIEW\
","\
ریکاوری و ارزیابی مجدد\
"))
           .append(p.sleepHours>0
               ? tr(p,"Sleep reported: ","خواب گزارش‌شده: ")+round(p.sleepHours)+tr(p," h/night. "," ساعت در شب. ")
               : tr(p,"Sleep duration not provided. ","مدت خواب وارد نشده است. "))
           .append(tr(p,"Aim toward 7–9 hours where feasible. Keep at least one recovery day each week. Review performance, body-weight trend, pain, sleep and session effort every 2 weeks; adjust volume or energy intake gradually.\
",
               "در صورت امکان به ۷ تا ۹ ساعت خواب نزدیک شوید. هر هفته دست‌کم یک روز ریکاوری داشته باشید. عملکرد، روند وزن، درد، خواب و سختی تمرین را هر دو هفته بررسی و تغییرات را تدریجی اعمال کنید.\
"));
        if(a.needsMedicalReview) out.append(tr(p,"\
MEDICAL NOTE — FOLLOW YOUR CLINICIAN’S RESTRICTIONS\
","\
یادآوری پزشکی — محدودیت‌های متخصص را رعایت کنید\
")).append(a.medicalReason).append("\
");
        if(a.bmi<18.5) out.append(tr(p,"\
BMI screening is below 18.5; do not use a fat-loss target. Seek a clinician/dietitian assessment.\
",
            "\
BMI کمتر از ۱۸٫۵ است؛ هدف کاهش وزن اعمال نشود و ارزیابی پزشک یا متخصص تغذیه انجام شود.\
"));
        if(a.bmi>=30) out.append(tr(p,"\
BMI is 30 or above; BMI alone is not a diagnosis. Progress gradually and seek clinical guidance if symptoms or comorbidities are present.\
",
            "\
BMI برابر یا بیشتر از ۳۰ است؛ این شاخص به‌تنهایی تشخیص پزشکی نیست. تدریجی پیش بروید و در صورت وجود علائم یا بیماری همراه، راهنمایی بالینی بگیرید.\
"));
        return out.toString();
    }

    public static String nutritionReport(Profile p, Analysis a) {
        validate(p);
        List<Meal> meals=createMeals(p,a);
        StringBuilder out=new StringBuilder();
        out.append(tr(p,"DAILY NUTRITION TARGET (ESTIMATE)\
","هدف روزانه تغذیه (تخمینی)\
"))
           .append(tr(p,"Energy: ","انرژی: ")).append(Math.round(a.targetCalories)).append(" kcal\
")
           .append(tr(p,"Protein: ","پروتئین: ")).append(Math.round(a.proteinGrams))
           .append(tr(p," g | Carbohydrate: "," گرم | کربوهیدرات: ")).append(Math.round(a.carbohydrateGrams))
           .append(tr(p," g | Fat: "," گرم | چربی: ")).append(Math.round(a.fatGrams)).append(" g\
")
           .append(tr(p,"Values are estimates; brands, cooking method, water loss and edible portion change actual values. Weigh rice, potato and meat in the stated cooked state.\
\
",
               "مقادیر تخمینی‌اند و با برند، روش پخت، کاهش آب و بخش خوراکی تغییر می‌کنند. برنج، سیب‌زمینی و گوشت را مطابق وضعیت پخته‌شده ذکرشده وزن کنید.\
\
"));
        if(a.needsMedicalReview&&!p.medicalClearance) {
            out.append(tr(p,"\
MEDICAL/DIETARY REVIEW REQUIRED\
","\
پیش از ساخت منوی غذایی، بررسی پزشکی/تغذیه‌ای لازم است\
"))
               .append(a.medicalReason).append("\
")
               .append(tr(p,"No individualized food menu has been generated until a qualified clinician/dietitian reviews the reported condition or medication.\
",
                   "تا زمان بررسی شرایط یا دارو توسط پزشک یا متخصص تغذیه، منوی غذایی اختصاصی تولید نمی‌شود.\
"));
            return out.toString();
        }
        double totalK=0,totalP=0,totalC=0,totalF=0;
        for(Meal meal:meals) {
            double mk=0,mp=0,mc=0,mf=0;
            out.append(meal.title).append("\
");
            for(Portion portion:meal.portions) {
                if(portion.grams<=0) continue;
                mk+=portion.kcal();mp+=portion.protein();mc+=portion.carbs();mf+=portion.fat();
                String amount;
                if("egg whites".equals(portion.food.unit)) amount=Math.max(1,Math.round(portion.grams/portion.food.unitGrams))
                    +(p.english?" egg whites (~":" عدد سفیده تخم‌مرغ (حدود ")+Math.round(portion.grams)+(p.english?" g)":" گرم)");
                else amount=Math.round(portion.grams)+(p.english?" g":" گرم");
                out.append("• ").append(displayFoodName(p,portion.food.name)).append(": ").append(amount)
                   .append(" — ").append(Math.round(portion.kcal())).append(tr(p," kcal\
"," کیلوکالری\
"));
            }
            totalK+=mk;totalP+=mp;totalC+=mc;totalF+=mf;
            out.append(tr(p,"Meal subtotal: ","جمع این وعده: ")).append(Math.round(mk)).append(" kcal | P ").append(Math.round(mp))
               .append(" g | C ").append(Math.round(mc)).append(" g | F ").append(Math.round(mf)).append(" g\
\
");
        }
        out.append(tr(p,"DAILY MENU TOTAL: ","مجموع کل منوی روز: ")).append(Math.round(totalK)).append(" kcal | ")
           .append(tr(p,"Protein ","پروتئین ")).append(Math.round(totalP))
           .append(" g | ").append(tr(p,"Carbs ","کربوهیدرات ")).append(Math.round(totalC))
           .append(" g | ").append(tr(p,"Fat ","چربی ")).append(Math.round(totalF)).append(" g\
")
           .append(tr(p,"Adjust portions after 2–3 weeks using average weight trend, training performance, hunger and recovery. Calorie values are not laboratory-exact.\
",
               "پس از ۲ تا ۳ هفته با توجه به میانگین روند وزن، عملکرد تمرین، گرسنگی و ریکاوری مقدارها را تنظیم کنید. کالری‌ها مقدار آزمایشگاهی دقیق نیستند.\
"));
        if(a.needsMedicalReview) out.append(tr(p,"\
MEDICAL/DIETARY NOTE — FOLLOW YOUR CLINICIAN’S RESTRICTIONS\
","\
یادآوری پزشکی/تغذیه‌ای — محدودیت‌های متخصص را رعایت کنید\
")).append(a.medicalReason).append("\
");
        if(!blank(p.allergies) && !isNone(p.allergies)) out.append(tr(p,"\
ALLERGY NOTE: ","\
هشدار حساسیت غذایی: ")).append(p.allergies)
           .append(tr(p,". Recognized matching foods are omitted, but ingredients and cross-contamination cannot be verified offline. Check labels and consult a dietitian for replacements.\
",
               "؛ موارد شناخته‌شده حذف می‌شوند، اما ترکیبات و آلودگی متقاطع به‌صورت آفلاین قابل بررسی نیست. برچسب محصولات را بررسی و برای جایگزین با متخصص تغذیه مشورت کنید.\
"));
        if(!blank(p.dislikedFoods) && !isNone(p.dislikedFoods)) out.append(tr(p,"Foods to avoid/replace: ","غذاهای حذف‌شده یا نامطلوب: ")).append(p.dislikedFoods).append("\
");
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
        Food chickpeas=food("Chickpeas (cooked)",164,8.9,27.4,2.6,0,"g",0);
        Food pumpkinSeeds=food("Pumpkin seeds",559,30.2,10.7,49.1,0,"g",0);
        Meal breakfast=new Meal(tr(p,"Breakfast","صبحانه"));
        breakfast.add(oats,50);breakfast.add(egg,132);breakfast.add(banana,100);
        Meal lunch=new Meal(tr(p,"Lunch","ناهار"));
        lunch.add(chicken,150);lunch.add(rice,180);lunch.add(veg,150);lunch.add(chickpeas,200);lunch.add(oil,5);
        Meal snack=new Meal(tr(p,"Snack","میان‌وعده"));
        snack.add(yogurt,200);snack.add(apple,150);snack.add(almonds,10);snack.add(pumpkinSeeds,20);
        Meal dinner=new Meal(tr(p,"Dinner","شام"));
        dinner.add(beef,130);dinner.add(potato,200);dinner.add(veg,150);dinner.add(oil,5);
        List<Meal> result=new ArrayList<>();Collections.addAll(result,breakfast,lunch,snack,dinner);
        if(!"omnivore".equals(p.dietaryPreference)) {
            // Vegetarian/vegan profiles must not silently receive meat or animal-derived foods.
            for(Meal m:result) {
                for(int i=m.portions.size()-1;i>=0;i--) {
                    String name=m.portions.get(i).food.name;
                    boolean animal="Chicken breast (cooked)".equals(name)||"Lean beef (cooked)".equals(name);
                    boolean veganOnly="vegan".equals(p.dietaryPreference)
                            &&("Egg whites".equals(name)||"Plain Greek yogurt, low-fat".equals(name));
                    if(animal||veganOnly)m.portions.remove(i);
                }
            }
        }
        // Include plant-protein choices in every menu so common meat exclusions
        // do not leave an unusable low-protein plan.
        result.get(1).add(food("Lentils (cooked)",116,9,20.1,0.4,0,"g",0),180);
        result.get(3).add(food("Firm tofu",144,17.3,2.8,8.7,0,"g",0),150);
        if("vegan".equals(p.dietaryPreference)) {
            result.get(2).add(food("Unsweetened soy yogurt",50,4,3,2.5,0,"g",0),200);
        }
        String allergy=p.allergies==null?"":p.allergies.toLowerCase(Locale.ROOT);
        String dislikes=p.dislikedFoods==null?"":p.dislikedFoods.toLowerCase(Locale.ROOT);
        for(Meal m:result) {
            for(int i=m.portions.size()-1;i>=0;i--) {
                String name=m.portions.get(i).food.name;
                if(allergyMatches(allergy,name)||(!blank(dislikes)&&!isNone(dislikes)&&isDisliked(dislikes,name))) {
                    m.portions.remove(i);
                }
            }
        }
        double baseProtein=0,baseFat=0,baseCarbs=0;
        for(Meal m:result)for(Portion x:m.portions){baseProtein+=x.protein();baseFat+=x.fat();baseCarbs+=x.carbs();}
        double pFactor=baseProtein<=0?1:clamp(a.proteinGrams/baseProtein,0.20,1.8);
        double fFactor=baseFat<=0?1:clamp(a.fatGrams/baseFat,0.20,1.8);
        for(Meal m:result) {
            for(int i=0;i<m.portions.size();i++) {
                Portion x=m.portions.get(i);
                double factor=x.food.group==0?pFactor:x.food.group==2?fFactor:1.0;
                double grams=x.grams*factor;
                if("egg whites".equals(x.food.unit)) grams=Math.max(x.food.unitGrams,Math.round(grams/x.food.unitGrams)*x.food.unitGrams);
                m.portions.set(i,new Portion(x.food,grams));
            }
        }
        double currentK=0,carbK=0;
        for(Meal m:result)for(Portion x:m.portions){currentK+=x.kcal();if(x.food.group==1)carbK+=x.kcal();}
        double carbFactor=carbK<=0?1:clamp((a.targetCalories-currentK+carbK)/carbK,0.20,2.5);
        for(Meal m:result) {
            for(int i=0;i<m.portions.size();i++) {
                Portion x=m.portions.get(i);
                if(x.food.group==1)m.portions.set(i,new Portion(x.food,x.grams*carbFactor));
            }
        }
        double menuKcal=0,menuProtein=0,menuFat=0;
        for(Meal meal:result) {
            if(meal.portions.isEmpty()) {
                throw new IllegalStateException(tr(p,"A meal has no safe foods after exclusions. Adjust the allergy/dislike list with a dietitian.",
                        "پس از حذف مواد غذایی، یک وعده بدون غذای ایمن باقی مانده است؛ فهرست حساسیت‌ها و غذاهای حذف‌شده را با متخصص تغذیه بازبینی کنید."));
            }
            for(Portion portion:meal.portions) {
                menuKcal+=portion.kcal();menuProtein+=portion.protein();menuFat+=portion.fat();
            }
        }
        if(menuProtein<a.proteinGrams*0.85||menuProtein>a.proteinGrams*1.30) {
            throw new IllegalStateException(tr(p,"The remaining safe foods cannot meet the estimated protein target. Add suitable tolerated protein foods with a dietitian.",
                    "غذاهای باقی‌مانده پروتئین هدف تخمینی را تأمین نمی‌کنند؛ با کمک متخصص تغذیه منابع پروتئینی قابل‌تحمل دیگری اضافه کنید."));
        }
        if(menuFat<a.fatGrams*0.70||menuFat>a.fatGrams*1.50) {
            throw new IllegalStateException(tr(p,"The remaining foods cannot provide a balanced fat target. Review exclusions with a dietitian.",
                    "غذاهای باقی‌مانده چربی هدف را تأمین نمی‌کنند؛ فهرست حذف‌ها را با متخصص تغذیه بازبینی کنید."));
        }
        if(menuKcal<a.targetCalories*0.85||menuKcal>a.targetCalories*1.15) {
            throw new IllegalStateException(tr(p,"The available food choices cannot produce a menu close to your calorie target. Add more tolerated foods.",
                    "با غذاهای انتخاب‌شده نمی‌توان منویی نزدیک به هدف کالری ساخت؛ مواد غذایی قابل‌تحمل بیشتری مشخص کنید."));
        }
        if(p.mealsPerDay==3) {
            Meal combined=new Meal(tr(p,"Breakfast + snack","صبحانه و میان‌وعده"));
            combined.portions.addAll(result.get(0).portions);
            combined.portions.addAll(result.get(2).portions);
            result.remove(2);result.remove(0);result.add(0,combined);
        } else if(p.mealsPerDay==5) {
            Meal first=new Meal(tr(p,"Snack 1","میان‌وعده اول"));
            Meal second=new Meal(tr(p,"Snack 2","میان‌وعده دوم"));
            for(Portion portion:result.get(2).portions) {
                first.add(portion.food,portion.grams/2.0);
                second.add(portion.food,portion.grams/2.0);
            }
            result.remove(2);result.add(2,first);result.add(3,second);
        }
        return result;
    }

    private static boolean allergyMatches(String allergy,String food) {
        String n=food.toLowerCase(Locale.ROOT);
        if(contains(allergy,"egg","تخم مرغ","تخم‌مرغ") && n.contains("egg")) return true;
        if(contains(allergy,"milk","dairy","lactose","casein","whey","لبنیات","شیر") && (n.contains("yogurt")||n.contains("milk")) && !n.contains("soy yogurt")) return true;
        if(contains(allergy,"nut","almond","peanut","بادام","آجیل","مغزها","بادام زمینی") && n.contains("almond")) return true;
        if(contains(allergy,"soy","سویا") && (n.contains("tofu")||n.contains("soy"))) return true;
        if(contains(allergy,"seed","pumpkin","تخمه","دانه") && n.contains("pumpkin seeds")) return true;
        if(contains(allergy,"beef","گوشت گاو","گوشت قرمز") && n.contains("beef")) return true;
        if(contains(allergy,"chicken","poultry","مرغ") && n.contains("chicken")) return true;
        if(contains(allergy,"gluten","wheat","گندم","گلوتن") && n.contains("oats")) return true;
        return false;
    }
    private static boolean isDisliked(String dislikes,String name) {
        String n=name.toLowerCase(Locale.ROOT);
        if(n.contains("egg")) return contains(dislikes,"egg","تخم مرغ","تخم‌مرغ");
        if(n.contains("chicken")) return contains(dislikes,"chicken","poultry","meat","گوشت","مرغ");
        if(n.contains("beef")) return contains(dislikes,"beef","meat","گوشت","گوشت قرمز","گوشت گوساله","گوشت گاو");
        if(n.contains("oats")) return contains(dislikes,"oats","oat","gluten","wheat","جو دوسر","جوپرک","جو پرک","گندم","گلوتن");
        if(n.contains("yogurt")) return contains(dislikes,"yogurt","dairy","milk","لبنیات","شیر","ماست");
        if(n.contains("almond")) return contains(dislikes,"almond","بادام","آجیل");
        if(n.contains("tofu")||n.contains("soy")) return contains(dislikes,"tofu","soy","توفو","سویا");
        if(n.contains("chickpeas")) return contains(dislikes,"chickpea","chickpeas","حمص","نخود");
        if(n.contains("pumpkin seeds")) return contains(dislikes,"pumpkin","seed","تخمه","تخمه کدو");
        if(n.contains("lentil")) return contains(dislikes,"lentil","عدس");
        if(n.contains("rice")) return contains(dislikes,"rice","برنج");
        if(n.contains("potato")) return contains(dislikes,"potato","سیب زمینی","سیب‌زمینی");
        if(n.contains("banana")) return contains(dislikes,"banana","موز");
        if(n.contains("apple")) return contains(dislikes,"apple","سیب");
        if(n.contains("olive oil")) return contains(dislikes,"olive oil","oil","روغن زیتون","روغن");
        if(n.contains("vegetables")) return contains(dislikes,"vegetable","greens","سبزی","سبزیجات");
        return contains(dislikes,n);
    }
    private static String foodKeyword(String name) {
        String n=name.toLowerCase(Locale.ROOT);
        if(n.contains("egg")) return "egg";
        if(n.contains("chicken")) return "chicken";
        if(n.contains("beef")) return "beef";
        if(n.contains("oats")) return "oats";
        if(n.contains("yogurt")) return "yogurt";
        if(n.contains("almond")) return "almond";
        if(n.contains("tofu")) return "tofu";
        if(n.contains("lentil")) return "lentil";
        if(n.contains("rice")) return "rice";
        if(n.contains("potato")) return "potato";
        if(n.contains("banana")) return "banana";
        if(n.contains("apple")) return "apple";
        return n;
    }
    private static boolean isNone(String value) {
        String v=value==null?"":value.trim().toLowerCase(Locale.ROOT);
        return v.equals("none")||v.equals("no")||v.equals("n/a")||v.equals("na")||v.equals("ندارد")||v.equals("ندارم")||v.equals("هیچ")||v.equals("خیر")||v.equals("هیچکدام")||v.equals("هیچ‌کدام")||v.equals("بدون مورد")||v.equals("بدون بیماری")||v.equals("بدون آسیب");
    }
    private static Food food(String name,double kcal,double protein,double carbs,double fat,double unit,String unitName,int group) {
        return new Food(name,kcal,protein,carbs,fat,unit,unitName,group);
    }
    private static String splitKey(Profile p,int day) {
        if(p.trainingDays<=3) return "full";
        if(p.trainingDays==5) return day==1?"push":day==2?"pull":day==3?"legs":day==4?"upper":"lower";
        return day%2==1?"upper":"lower";
    }
    private static String splitLabel(Profile p,String key) {
        switch(key) {
            case "upper": return tr(p,"Upper body","بالاتنه");
            case "lower": return tr(p,"Lower body","پایین‌تنه");
            case "push": return tr(p,"Push","فشاری");
            case "pull": return tr(p,"Pull","کششی");
            case "legs": return tr(p,"Legs & core","پا و میان‌تنه");
            default: return tr(p,"Full body","کل بدن");
        }
    }
    private static boolean matchesSplit(String split,String category) {
        if("full".equals(split)) return true;
        if("upper".equals(split)) return "push".equals(category)||"pull".equals(category)||"other".equals(category);
        if("lower".equals(split)||"legs".equals(split)) return "legs".equals(category)||"glutes".equals(category)||"core".equals(category);
        if("push".equals(split)) return "push".equals(category)||"other".equals(category);
        if("pull".equals(split)) return "pull".equals(category)||"other".equals(category);
        return true;
    }
    private static void appendExercise(StringBuilder out,Profile p,PersonalPlanGenerator.PlannedExercise item,int index) {
        out.append("  ").append(index).append(". ")
           .append(p.english?item.asset.name:ExerciseNameLocalizer.toPersian(item.asset.name))
           .append(" — ").append(item.sets).append(tr(p," sets × "," ست × "))
           .append(item.reps).append(tr(p," reps\
"," تکرار\
"));
    }

    private static String tr(Profile p,String english,String persian){return p.english?english:persian;}
    private static String displayFoodName(Profile p,String name) {
        if(p.english)return name;
        switch(name) {
            case "Rolled oats (dry)": return "جو دوسر پرک (خشک)";
            case "Egg whites": return "سفیده تخم‌مرغ";
            case "Banana (edible)": return "موز (قسمت خوراکی)";
            case "Chicken breast (cooked)": return "سینه مرغ (پخته)";
            case "White rice (cooked)": return "برنج سفید (پخته)";
            case "Mixed non-starchy vegetables": return "سبزیجات غیرنشاسته‌ای مخلوط";
            case "Olive oil": return "روغن زیتون";
            case "Plain Greek yogurt, low-fat": return "ماست یونانی ساده کم‌چرب";
            case "Apple (edible)": return "سیب (قسمت خوراکی)";
            case "Almonds": return "بادام";
            case "Lean beef (cooked)": return "گوشت گوساله کم‌چرب (پخته)";
            case "Potato (boiled)": return "سیب‌زمینی آب‌پز";
            case "Lentils (cooked)": return "عدس پخته";
            case "Firm tofu": return "توفوی سفت";
            case "Unsweetened soy yogurt": return "ماست سویای بدون شکر";
            case "Chickpeas (cooked)": return "نخود پخته";
            case "Pumpkin seeds": return "تخمه کدو";
            default: return name;
        }
    }
    private static boolean contains(String value,String... terms) {
        String source=value==null?"":value.toLowerCase(Locale.ROOT);
        for(String term:terms) if(source.contains(term.toLowerCase(Locale.ROOT))) return true;
        return false;
    }
    private static boolean blank(String s){return s==null||s.trim().isEmpty();}
    private static double clamp(double v,double min,double max){return Math.max(min,Math.min(max,v));}
    private static double round(double v){return Math.round(v*10.0)/10.0;}
}
