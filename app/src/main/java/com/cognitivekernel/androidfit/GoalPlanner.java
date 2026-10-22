package com.cognitivekernel.androidfit;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.text.InputType;
import android.view.ViewGroup;
import android.widget.*;
import java.util.Locale;

/** Offline educational workout and nutrition guide. */
public final class GoalPlanner {
 private GoalPlanner() {}
 private static String t(Activity a,String en,String fa){return a.getSharedPreferences("fit_settings",0).getString("language","fa").equals("en")?en:fa;}
 private static EditText input(Activity a,String hint,String value){EditText e=new EditText(a);e.setHint(hint);e.setText(value);e.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);return e;}
 private static Spinner choices(Activity a,String[] values){Spinner s=new Spinner(a);s.setAdapter(new ArrayAdapter<>(a,android.R.layout.simple_spinner_dropdown_item,values));return s;}
 public static void show(Activity a){
  LinearLayout form=new LinearLayout(a);form.setOrientation(LinearLayout.VERTICAL);int p=(int)(14*a.getResources().getDisplayMetrics().density);form.setPadding(p,p,p,p);
  Spinner goal=choices(a,new String[]{t(a,"Weight loss","کاهش وزن"),t(a,"Fitness & body composition","فیتنس و تناسب اندام"),t(a,"Muscle building","عضله‌سازی")});
  Spinner area=choices(a,new String[]{t(a,"Full body","کل بدن"),t(a,"Shoulders","سرشانه"),t(a,"Arms","بازو"),t(a,"Abs / core","شکم و میان‌تنه"),t(a,"Legs & glutes","پا و باسن"),t(a,"Chest","سینه"),t(a,"Back","پشت و زیربغل")});
  Spinner sex=choices(a,new String[]{t(a,"Male","مرد"),t(a,"Female","زن")});
  Spinner activity=choices(a,new String[]{t(a,"Low activity","فعالیت کم"),t(a,"Moderate activity","فعالیت متوسط"),t(a,"High activity","فعالیت زیاد")});
  EditText age=input(a,t(a,"Age (years)","سن (سال)"),"30"),height=input(a,t(a,"Height (cm)","قد (سانتی‌متر)"),"170"),weight=input(a,t(a,"Weight (kg)","وزن (کیلوگرم)"),"70");
  form.addView(goal);form.addView(new TextView(a){{setText(t(a,"Muscle target area","ناحیه هدف عضله‌سازی"));}});form.addView(area);form.addView(sex);form.addView(age);form.addView(height);form.addView(weight);form.addView(activity);
  new AlertDialog.Builder(a).setTitle(t(a,"Personal training & nutrition","تمرین و تغذیه شخصی")).setView(form).setPositiveButton(t(a,"Create guide","ساخت راهنما"),(dialog,which)->{
   try{
    int years=Integer.parseInt(age.getText().toString().trim());double cm=Double.parseDouble(height.getText().toString().trim()),kg=Double.parseDouble(weight.getText().toString().trim());
    if(years<18||years>100||cm<120||cm>230||kg<30||kg>300)throw new IllegalArgumentException();
    int g=goal.getSelectedItemPosition(),r=area.getSelectedItemPosition(),sx=sex.getSelectedItemPosition(),act=activity.getSelectedItemPosition();
    double bmi=kg/Math.pow(cm/100.0,2),bmr=10*kg+6.25*cm-5*years+(sx==0?5:-161),maint=bmr*(act==0?1.35:act==1?1.55:1.725),target=Math.max(1200,g==0?maint-350:g==1?maint:maint+200),protein=kg*(g==2?1.8:1.5);
    String[] body={t(a,"full body: squat, push-up, row and plank","کل بدن: اسکوات، شنا، پارویی و پلانک"),t(a,"shoulders: overhead press, lateral raise, rear-delt raise","سرشانه: پرس سرشانه، نشر جانب و نشر خم"),t(a,"arms: biceps curl, hammer curl, triceps extension","بازو: جلو بازو، چکشی و پشت بازو"),t(a,"abs/core: plank, dead bug, reverse crunch; visible abs also depend on overall body fat","شکم: پلانک، ددباگ و کرانچ معکوس؛ نمایان‌شدن عضلات شکم به چربی کل بدن هم بستگی دارد"),t(a,"legs/glutes: squat, lunge, hip hinge and glute bridge","پا و باسن: اسکوات، لانج، هیپ‌هینج و پل باسن"),t(a,"chest: push-ups and dumbbell/floor press","سینه: شنا و پرس دمبل یا پرس روی زمین"),t(a,"back: rows, band rows and safe pulling exercises","پشت و زیربغل: انواع پارویی، پارویی کشی و حرکات کششی ایمن")};
    String title,training,food;
    if(g==0){title=t(a,"Weight loss plan","برنامه کاهش وزن");training=t(a,"3–5 days/week: 25–45 min brisk walking/cycling plus 2–3 full-body strength sessions. Increase activity gradually.","۳ تا ۵ روز در هفته: ۲۵ تا ۴۵ دقیقه پیاده‌روی تند یا دوچرخه و ۲ تا ۳ جلسه قدرتی کل بدن؛ فعالیت را تدریجی زیاد کنید.");food=t(a,"Modest calorie deficit; prioritize vegetables, fruit, beans, whole grains and lean protein; reduce sugary drinks and highly processed snacks.","کسری کالری ملایم؛ سبزی، میوه، حبوبات، غلات کامل و پروتئین کم‌چرب بیشتر؛ نوشیدنی شیرین و تنقلات بسیار فرآوری‌شده کمتر.");}
    else if(g==1){title=t(a,"Fitness plan","برنامه فیتنس");training=t(a,"3 strength sessions/week for major muscle groups, 2 moderate cardio sessions and mobility work. Start with 2–3 sets of 8–15 controlled reps.","۳ جلسه قدرتی برای عضلات اصلی، ۲ جلسه هوازی متوسط و تمرین تحرک؛ با ۲ تا ۳ ست و ۸ تا ۱۵ تکرار کنترل‌شده شروع کنید.");food=t(a,"Start near estimated maintenance; include protein at each meal, vegetables, whole-food carbohydrates and healthy fats. Review after 2–3 weeks.","نزدیک کالری نگهدارنده تخمینی شروع کنید؛ هر وعده پروتئین، سبزی، کربوهیدرات کامل و چربی مفید داشته باشد؛ پس از ۲ تا ۳ هفته ارزیابی کنید.");}
    else{title=t(a,"Muscle-building plan","برنامه عضله‌سازی");training=t(a,"Target: ","هدف: ")+body[r]+"\n"+t(a,"Train the chosen area twice weekly with about 48 hours between hard sessions. Begin with 3 exercises, 2–4 sets each and 6–15 reps; increase reps or load gradually.","ناحیه انتخابی را هفته‌ای ۲ بار با حدود ۴۸ ساعت فاصله تمرین دهید. با ۳ حرکت، هرکدام ۲ تا ۴ ست و ۶ تا ۱۵ تکرار شروع کنید و بار را تدریجی افزایش دهید.");food=t(a,"Start near maintenance or a small surplus; spread protein across 3–4 meals, eat enough carbohydrates for training and include vegetables, fruit and healthy fats.","نزدیک کالری نگهدارنده یا با مازاد اندک شروع کنید؛ پروتئین را در ۳ تا ۴ وعده تقسیم کنید، کربوهیدرات کافی بخورید و سبزی، میوه و چربی مفید مصرف کنید.");}
    String result=t(a,"Estimated BMI (screening only)","BMI تخمینی (فقط غربالگری)")+": "+String.format(Locale.getDefault(),"%.1f",bmi)+"\n"+t(a,"Estimated maintenance","کالری نگهدارنده تخمینی")+": "+Math.round(maint)+" kcal/day\n"+t(a,"Starting energy target","هدف اولیه انرژی")+": "+Math.round(target)+" kcal/day\n"+t(a,"Protein guide","راهنمای پروتئین")+": "+Math.round(protein)+" g/day\n\n"+t(a,"TRAINING","تمرین")+"\n"+training+"\n\n"+t(a,"NUTRITION","تغذیه")+"\n"+food+"\n\n"+t(a,"Safety: rough estimates for adults only; medical conditions, medicines, pregnancy and eating-disorder history are not accounted for. Seek professional advice when needed.","ایمنی: تخمین‌های تقریبی ویژه بزرگسالان است و بیماری، دارو، بارداری یا سابقه اختلال خوردن را لحاظ نمی‌کند. در صورت نیاز با متخصص مشورت کنید.");
    LinearLayout resultView=new LinearLayout(a);resultView.setOrientation(LinearLayout.VERTICAL);int pad=(int)(14*a.getResources().getDisplayMetrics().density);resultView.setPadding(pad,pad,pad,pad);
    ImageView illustration=new ImageView(a);int art=g==0?R.drawable.exercise_walk:g==1?R.drawable.exercise_squat:(r==1?R.drawable.exercise_pushup:r==2?R.drawable.exercise_row:r==3?R.drawable.exercise_plank:r==4?R.drawable.exercise_squat:r==5?R.drawable.exercise_pushup:r==6?R.drawable.exercise_row:R.drawable.exercise_squat);illustration.setImageResource(art);illustration.setContentDescription(title);illustration.setAdjustViewBounds(true);illustration.setMaxHeight((int)(170*a.getResources().getDisplayMetrics().density));resultView.addView(illustration,new LinearLayout.LayoutParams(-1,(int)(150*a.getResources().getDisplayMetrics().density)));
    TextView details=new TextView(a);details.setText(result);details.setTextSize(16);resultView.addView(details);
    ScrollView scroll=new ScrollView(a);scroll.addView(resultView);new AlertDialog.Builder(a).setTitle(title).setView(scroll).setPositiveButton(android.R.string.ok,null).show();
   }catch(Exception e){new AlertDialog.Builder(a).setMessage(t(a,"Enter age 18–100, height 120–230 cm and weight 30–300 kg.","سن ۱۸ تا ۱۰۰، قد ۱۲۰ تا ۲۳۰ سانتی‌متر و وزن ۳۰ تا ۳۰۰ کیلوگرم وارد کنید.")).setPositiveButton(android.R.string.ok,null).show();}
  }).setNegativeButton(android.R.string.cancel,null).show();
 }
}