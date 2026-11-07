package com.cognitivekernel.androidfit;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.text.InputType;
import android.view.View;
import android.widget.ArrayAdapter;
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
        LinearLayout form=new LinearLayout(a);form.setOrientation(LinearLayout.VERTICAL);
        int pad=(int)(16*a.getResources().getDisplayMetrics().density);form.setPadding(pad,pad,pad,pad);
        Spinner goal=spinner(a,new String[]{t(a,"Strength","قدرت"),t(a,"Muscle building","عضله‌سازی"),t(a,"General fitness","تناسب اندام"),t(a,"Fat loss","کاهش چربی")});
        Spinner sex=spinner(a,new String[]{t(a,"Male","مرد"),t(a,"Female","زن")});
        Spinner activity=spinner(a,new String[]{t(a,"Sedentary","کم‌تحرک"),t(a,"Lightly active","فعالیت سبک"),t(a,"Moderately active","فعالیت متوسط"),t(a,"Very active","فعالیت زیاد")});
        Spinner diet=spinner(a,new String[]{t(a,"Omnivore","همه‌چیزخوار"),t(a,"Vegetarian","گیاه‌خوار"),t(a,"Vegan","وگان")});
        EditText age=input(a,t(a,"Age (18–100 years)","سن (۱۸ تا ۱۰۰ سال)"),String.valueOf(saved.age),InputType.TYPE_CLASS_NUMBER);
        EditText height=input(a,t(a,"Height (cm)","قد (سانتی‌متر)"),String.valueOf(saved.heightCm),InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
        EditText weight=input(a,t(a,"Current weight (kg)","وزن فعلی (کیلوگرم)"),String.valueOf(saved.weightKg),InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
        EditText conditions=input(a,t(a,"Medical conditions (or 'none')","بیماری‌ها و شرایط پزشکی (یا «ندارد»)"),saved.conditions,InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        EditText injuries=input(a,t(a,"Pain, injuries, surgeries or movement restrictions","درد، آسیب، جراحی یا محدودیت حرکتی"),saved.injuries,InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        EditText medications=input(a,t(a,"Medicines and supplements","داروها و مکمل‌های مصرفی"),saved.medications,InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        EditText allergies=input(a,t(a,"Food allergies / intolerances","حساسیت یا عدم تحمل غذایی"),saved.allergies,InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        EditText dislikes=input(a,t(a,"Foods to avoid / dislike","غذاهایی که مصرف نمی‌کنید یا دوست ندارید"),saved.dislikedFoods,InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        for(EditText e:new EditText[]{conditions,injuries,medications,allergies,dislikes}){e.setSingleLine(false);e.setMinLines(2);e.setMaxLines(4);}
        goal.setSelection(saved.goalIndex);sex.setSelection(saved.sexIndex);activity.setSelection(saved.activityIndex);
        diet.setSelection("vegan".equals(saved.dietaryPreference)?2:"vegetarian".equals(saved.dietaryPreference)?1:0);
        add(form,a,t(a,"Goal","هدف"),goal);add(form,a,t(a,"Sex for energy estimate","جنسیت برای تخمین انرژی"),sex);
        add(form,a,t(a,"Age","سن"),age);add(form,a,t(a,"Height","قد"),height);add(form,a,t(a,"Weight","وزن"),weight);
        add(form,a,t(a,"Daily activity","فعالیت روزانه"),activity);add(form,a,t(a,"Diet preference","الگوی غذایی"),diet);
        add(form,a,t(a,"Medical conditions (include pregnancy/breastfeeding if applicable)","شرایط پزشکی (در صورت ارتباط بارداری یا شیردهی را هم ذکر کنید)"),conditions);add(form,a,t(a,"Injuries and movement restrictions","آسیب‌ها و محدودیت‌های حرکتی"),injuries);add(form,a,t(a,"Medicines","داروها"),medications);
        add(form,a,t(a,"Allergies","حساسیت‌ها"),allergies);add(form,a,t(a,"Foods to avoid","غذاهای نامطلوب"),dislikes);
        ScrollView scroll=new ScrollView(a);scroll.addView(form);
        new AlertDialog.Builder(a).setTitle(t(a,"Personal nutrition plan","برنامه تغذیه شخصی")).setView(scroll)
          .setPositiveButton(t(a,"Calculate and create menu","محاسبه و ساخت برنامه غذایی"),(dialog,which)->{
            try {
                FitnessPlanEngine.Profile p=FitnessProfileStore.load(a);
                p.age=GoalPlanner.parseInteger(age.getText().toString());
                p.heightCm=GoalPlanner.parseDecimal(height.getText().toString());
                p.weightKg=GoalPlanner.parseDecimal(weight.getText().toString());
                p.sexIndex=sex.getSelectedItemPosition();p.goalIndex=goal.getSelectedItemPosition();
                p.activityIndex=activity.getSelectedItemPosition();
                p.dietaryPreference=diet.getSelectedItemPosition()==2?"vegan":diet.getSelectedItemPosition()==1?"vegetarian":"omnivore";
                p.conditions=conditions.getText().toString();p.injuries=injuries.getText().toString();p.medications=medications.getText().toString();
                p.allergies=allergies.getText().toString();p.dislikedFoods=dislikes.getText().toString();
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
                  .setMessage(t(a,"Enter age 18–100, height 120–230 cm and weight 30–300 kg using valid numbers.",
                    "سن ۱۸ تا ۱۰۰، قد ۱۲۰ تا ۲۳۰ سانتی‌متر و وزن ۳۰ تا ۳۰۰ کیلوگرم را با اعداد معتبر وارد کنید."))
                  .setPositiveButton(android.R.string.ok,null).show();
            }
          }).setNegativeButton(android.R.string.cancel,null).show();
    }
}
