package com.cognitivekernel.androidfit;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.text.InputType;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

/** Detailed offline food plan form backed by the shared user profile. */
public final class PersonalNutritionPlanner {
    private PersonalNutritionPlanner() {}

    private static String t(Activity a,String en,String fa) {
        String language=a.getSharedPreferences("fit_settings",Context.MODE_PRIVATE).getString("language","fa");
        return "en".equalsIgnoreCase(language)?en:fa;
    }
    private static EditText input(Activity a,String hint,String value,int type) {
        EditText e=new EditText(a);e.setHint(hint);e.setText(value==null?"":value);e.setInputType(type);return e;
    }
    private static Spinner spinner(Activity a,String[] values) {
        Spinner s=new Spinner(a);s.setAdapter(new ArrayAdapter<>(a,android.R.layout.simple_spinner_dropdown_item,values));return s;
    }
    private static void add(LinearLayout form,Activity a,String title,View view) {
        TextView label=new TextView(a);label.setText(title);label.setTextSize(15);form.addView(label);form.addView(view);
    }

    public static void show(Activity a) {
        FitnessPlanEngine.Profile saved=FitnessProfileStore.load(a);
        boolean hasProfile=FitnessProfileStore.hasProfile(a);
        LinearLayout form=new LinearLayout(a);form.setOrientation(LinearLayout.VERTICAL);
        int pad=(int)(16*a.getResources().getDisplayMetrics().density);form.setPadding(pad,pad,pad,pad);
        Spinner goal=spinner(a,new String[]{t(a,"Select a goal","هدف را انتخاب کنید"),t(a,"Strength","قدرت"),t(a,"Muscle building","عضله‌سازی"),t(a,"General fitness","تناسب اندام"),t(a,"Fat loss","کاهش چربی")});
        Spinner sex=spinner(a,new String[]{t(a,"Select sex","جنسیت را انتخاب کنید"),t(a,"Male","مرد"),t(a,"Female","زن"),t(a,"Other / prefer not to say","سایر / ترجیح می‌دهم اعلام نکنم")});
        Spinner level=spinner(a,new String[]{t(a,"Select experience","سطح تجربه را انتخاب کنید"),t(a,"Beginner","مبتدی"),t(a,"Intermediate","متوسط"),t(a,"Advanced","پیشرفته")});
        Spinner equipment=spinner(a,new String[]{t(a,"Select equipment","تجهیزات را انتخاب کنید"),t(a,"No equipment","بدون تجهیزات"),t(a,"Home equipment","تجهیزات خانگی"),t(a,"Full gym","باشگاه کامل")});
        Spinner activity=spinner(a,new String[]{t(a,"Select daily activity","فعالیت روزانه را انتخاب کنید"),t(a,"Sedentary","کم‌تحرک"),t(a,"Lightly active","فعالیت سبک"),t(a,"Moderately active","فعالیت متوسط"),t(a,"Very active","فعالیت زیاد")});
        Spinner diet=spinner(a,new String[]{t(a,"Select diet preference","الگوی غذایی را انتخاب کنید"),t(a,"Omnivore","همه‌چیزخوار"),t(a,"Vegetarian","گیاه‌خوار"),t(a,"Vegan","وگان")});
        Spinner meals=spinner(a,new String[]{t(a,"Select meals per day","تعداد وعده را انتخاب کنید"),t(a,"3 meals","۳ وعده"),t(a,"4 meals","۴ وعده"),t(a,"5 meals","۵ وعده")});
        EditText age=input(a,t(a,"Age (18–100 years)","سن (۱۸ تا ۱۰۰ سال)"),(hasProfile?String.valueOf(saved.age):""),InputType.TYPE_CLASS_NUMBER);
        EditText height=input(a,t(a,"Height (cm)","قد (سانتی‌متر)"),(hasProfile?String.valueOf(saved.heightCm):""),InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
        EditText weight=input(a,t(a,"Current weight (kg)","وزن فعلی (کیلوگرم)"),(hasProfile?String.valueOf(saved.weightKg):""),InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
        EditText days=input(a,t(a,"Training days per week (1–6)","روز تمرین در هفته (۱ تا ۶)"),(hasProfile?String.valueOf(saved.trainingDays):""),InputType.TYPE_CLASS_NUMBER);
        EditText minutes=input(a,t(a,"Session duration (10–180 min)","مدت هر جلسه (۱۰ تا ۱۸۰ دقیقه)"),(hasProfile?String.valueOf(saved.sessionMinutes):""),InputType.TYPE_CLASS_NUMBER);
        EditText sleep=input(a,t(a,"Average sleep hours per night (optional)","میانگین ساعت خواب شبانه (اختیاری)"),(saved.sleepHours>0?String.valueOf(saved.sleepHours):""),InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
        EditText conditions=input(a,t(a,"Medical conditions (or 'none')","بیماری‌ها و شرایط پزشکی (یا «ندارد»)"),saved.conditions,InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        EditText injuries=input(a,t(a,"Pain, injuries, surgeries or movement restrictions (or none)","درد، آسیب، جراحی یا محدودیت حرکتی (یا ندارد)"),saved.injuries,InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        EditText medications=input(a,t(a,"Medicines and supplements (write none if not applicable)","داروها و مکمل‌ها (در صورت نداشتن بنویسید: ندارد)"),saved.medications,InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        EditText allergies=input(a,t(a,"Food allergies / intolerances (or none)","حساسیت یا عدم تحمل غذایی (یا ندارد)"),saved.allergies,InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        EditText dislikes=input(a,t(a,"Foods to avoid / dislike","غذاهایی که مصرف نمی‌کنید یا دوست ندارید"),saved.dislikedFoods,InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        CheckBox clearance=new CheckBox(a);
        clearance.setText(t(a,"I have clinician clearance for nutrition with the conditions/medicines listed above","برای تغذیه با توجه به شرایط و داروهای ذکرشده، مجوز متخصص دارم"));
        clearance.setChecked(saved.medicalClearance);
        for(EditText e:new EditText[]{conditions,injuries,medications,allergies,dislikes}){e.setSingleLine(false);e.setMinLines(2);e.setMaxLines(4);}
        if(hasProfile) {
            goal.setSelection(saved.goalIndex+1);sex.setSelection(saved.sexIndex+1);activity.setSelection(saved.activityIndex+1);
            level.setSelection(saved.levelIndex+1);equipment.setSelection(saved.equipmentIndex+1);
            diet.setSelection("vegan".equals(saved.dietaryPreference)?3:"vegetarian".equals(saved.dietaryPreference)?2:1);
            meals.setSelection(Math.max(1,Math.min(3,saved.mealsPerDay-2)));
        }
        add(form,a,t(a,"Goal","هدف"),goal);add(form,a,t(a,"Sex for energy estimate","جنسیت برای تخمین انرژی"),sex);
        add(form,a,t(a,"Age","سن"),age);add(form,a,t(a,"Height","قد"),height);add(form,a,t(a,"Weight","وزن"),weight);
        add(form,a,t(a,"Daily activity","فعالیت روزانه"),activity);add(form,a,t(a,"Diet preference","الگوی غذایی"),diet);
        add(form,a,t(a,"Training experience","سطح تجربه تمرین"),level);add(form,a,t(a,"Available equipment","تجهیزات در دسترس"),equipment);
        add(form,a,t(a,"Training days per week","روز تمرین در هفته"),days);add(form,a,t(a,"Minutes per session","مدت هر جلسه"),minutes);
        add(form,a,t(a,"Sleep per night","خواب شبانه"),sleep);
        add(form,a,t(a,"Meals per day","تعداد وعده در روز"),meals);
        add(form,a,t(a,"Medical conditions (include pregnancy/breastfeeding if applicable)","شرایط پزشکی (در صورت ارتباط بارداری یا شیردهی را هم ذکر کنید)"),conditions);add(form,a,t(a,"Injuries and movement restrictions","آسیب‌ها و محدودیت‌های حرکتی"),injuries);add(form,a,t(a,"Medicines","داروها"),medications);
        add(form,a,t(a,"Allergies","حساسیت‌ها"),allergies);add(form,a,t(a,"Foods to avoid","غذاهای نامطلوب"),dislikes);
        add(form,a,t(a,"Medical clearance (required when health risks are reported)","تأییدیه پزشکی (در صورت وجود شرایط پزشکی لازم است)"),clearance);
        ScrollView scroll=new ScrollView(a);scroll.addView(form);
        new AlertDialog.Builder(a).setTitle(t(a,"Personal nutrition plan","برنامه تغذیه شخصی")).setView(scroll)
          .setPositiveButton(t(a,"Calculate and create menu","محاسبه و ساخت برنامه غذایی"),(dialog,which)->{
            try {
                FitnessPlanEngine.Profile p=FitnessProfileStore.load(a);
                p.age=GoalPlanner.parseInteger(age.getText().toString());
                p.heightCm=GoalPlanner.parseDecimal(height.getText().toString());
                p.weightKg=GoalPlanner.parseDecimal(weight.getText().toString());
                p.sexIndex=sex.getSelectedItemPosition()-1;p.goalIndex=goal.getSelectedItemPosition()-1;
                p.activityIndex=activity.getSelectedItemPosition()-1;
                p.levelIndex=level.getSelectedItemPosition()-1;p.equipmentIndex=equipment.getSelectedItemPosition()-1;
                p.trainingDays=GoalPlanner.parseInteger(days.getText().toString());
                p.sessionMinutes=GoalPlanner.parseInteger(minutes.getText().toString());
                p.sleepHours=sleep.getText().toString().trim().isEmpty()?0:GoalPlanner.parseDecimal(sleep.getText().toString());
                p.dietaryPreference=diet.getSelectedItemPosition()==3?"vegan":diet.getSelectedItemPosition()==2?"vegetarian":diet.getSelectedItemPosition()==1?"omnivore":"";
                p.mealsPerDay=2+meals.getSelectedItemPosition();
                p.conditions=conditions.getText().toString();p.injuries=injuries.getText().toString();p.medications=medications.getText().toString();
                p.allergies=allergies.getText().toString();p.dislikedFoods=dislikes.getText().toString();p.medicalClearance=clearance.isChecked();
                FitnessPlanEngine.validate(p);
                FitnessPlanEngine.Analysis analysis=FitnessPlanEngine.analyze(p);
                FitnessProfileStore.save(a,p);
                String report=FitnessPlanEngine.nutritionReport(p,analysis);
                TextView body=new TextView(a);body.setText(report);body.setTextSize(15);body.setPadding(pad,pad,pad,pad);
                ScrollView result=new ScrollView(a);result.addView(body);
                new AlertDialog.Builder(a).setTitle(t(a,"Your detailed food plan","برنامه غذایی دقیق شما"))
                  .setView(result).setPositiveButton(android.R.string.ok,null).show();
            } catch(Exception ex) {
                new AlertDialog.Builder(a).setTitle(t(a,"Check your information","اطلاعات را بررسی کنید"))
                  .setMessage(t(a,"Enter valid measurements, select every required option, and write none in medical, injury, medicine and allergy fields when not applicable.",
                    "اطلاعات قد، وزن و سن را معتبر وارد کنید، گزینه‌های لازم را انتخاب کنید و در صورت نداشتن بیماری، آسیب، دارو یا حساسیت، عبارت «ندارد» را بنویسید."))
                  .setPositiveButton(android.R.string.ok,null).show();
            }
          }).setNegativeButton(android.R.string.cancel,null).show();
    }
}
