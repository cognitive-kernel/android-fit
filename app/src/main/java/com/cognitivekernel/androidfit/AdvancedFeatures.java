package com.cognitivekernel.androidfit;

import android.app.*;
import android.content.*;
import android.database.Cursor;import android.database.sqlite.SQLiteDatabase;
import android.graphics.*;
import android.net.Uri;
import android.os.Build;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.*;
import org.json.JSONArray;
import org.json.JSONObject;

public final class AdvancedFeatures {
 private final Activity a; private final WorkoutDb db;
 private static final int BACKUP=4101, RESTORE=4102, NOTIFY=4103;
 public AdvancedFeatures(Activity activity, WorkoutDb database){a=activity;db=database;}
 private String s(String key){int id=a.getResources().getIdentifier(key,"string",a.getPackageName());return id==0?key:a.getString(id);}
 private String tr(String en,String fa){return a.getSharedPreferences("fit_settings",Context.MODE_PRIVATE).getString("language","fa").equals("en")?en:fa;}
 private EditText field(String hint,String value,int type){EditText e=new EditText(a);e.setHint(hint);if(value!=null)e.setText(value);e.setInputType(type);return e;}
 private static int parseLocalizedInt(String value){return GoalPlanner.parseInteger(value);}
 private static double parseLocalizedDouble(String value){return GoalPlanner.parseDecimal(value);}
 private LinearLayout column(){LinearLayout l=new LinearLayout(a);l.setOrientation(LinearLayout.VERTICAL);int p=(int)(16*a.getResources().getDisplayMetrics().density);l.setPadding(p,p,p,p);return l;}
 private void message(String title,String body){new AlertDialog.Builder(a).setTitle(title).setMessage(body).setPositiveButton(android.R.string.ok,null).show();}

 public void showHistory(){
  int logs=0,days=0;double volume=0,best=0;
  try(Cursor c=db.summary()){if(c.moveToFirst()){logs=c.getInt(0);days=c.getInt(1);volume=c.getDouble(2);best=c.getDouble(3);}}
  LinearLayout box=column();TextView summary=new TextView(a);
  summary.setText(s("progress_summary")+": "+logs+"\n"+s("training_days")+": "+days+"\n"+s("total_volume")+": "+String.format(Locale.getDefault(),"%.1f",volume)+" kg\n"+s("best_weight")+": "+String.format(Locale.getDefault(),"%.1f",best)+" kg");
  summary.setTextSize(16);box.addView(summary);
  TextView chartTitle=new TextView(a);chartTitle.setText(s("volume_chart"));chartTitle.setTextSize(18);box.addView(chartTitle);
  ArrayList<String> labels=new ArrayList<>();ArrayList<Float> values=new ArrayList<>();
  try(Cursor c=db.volumeByDate()){while(c.moveToNext()){labels.add(c.getString(0));values.add((float)c.getDouble(1));}}
  Collections.reverse(labels);Collections.reverse(values);box.addView(new VolumeChart(a,labels,values),new LinearLayout.LayoutParams(-1,(int)(170*a.getResources().getDisplayMetrics().density)));
  TextView recentTitle=new TextView(a);recentTitle.setText(s("recent_sessions"));recentTitle.setTextSize(18);box.addView(recentTitle);
  StringBuilder rows=new StringBuilder();
  try(Cursor c=db.recentLogs()){while(c.moveToNext()){rows.append(c.getString(0)).append(" · ").append(c.getString(1)).append("\n").append(c.getInt(2)).append(" × ").append(c.getInt(3)).append(" · ").append(c.getDouble(4)).append(" kg · RPE ").append(c.getInt(5));String n=c.getString(6);if(n!=null&&!n.isEmpty())rows.append("\n").append(n);rows.append("\n\n");}}
  TextView recent=new TextView(a);recent.setText(rows.length()==0?s("no_history"):rows.toString());box.addView(recent);
  ScrollView scroll=new ScrollView(a);scroll.addView(box);new AlertDialog.Builder(a).setTitle(s("history_progress")).setView(scroll).setPositiveButton(android.R.string.ok,null).show();
 }

 private static final class VolumeChart extends View {
  final Paint p=new Paint(3);final List<String> labels;final List<Float> values;
  VolumeChart(Activity a,List<String> l,List<Float> v){super(a);labels=l;values=v;}
  @Override protected void onDraw(Canvas c){super.onDraw(c);float w=getWidth(),h=getHeight(),bottom=h-28,top=12; p.setColor(Color.LTGRAY);p.setStrokeWidth(1);c.drawLine(8,bottom,w-8,bottom,p);if(values.isEmpty()){p.setColor(Color.GRAY);p.setTextSize(14);c.drawText("Log workouts to see progress",12,h/2,p);return;}float max=1;for(float v:values)max=Math.max(max,v);float slot=(w-20)/values.size();for(int i=0;i<values.size();i++){float bh=(bottom-top)*(values.get(i)/max);float left=10+i*slot+slot*.18f;p.setColor(Color.rgb(36,107,90));c.drawRoundRect(left,bottom-bh,left+slot*.64f,bottom,6,6,p);p.setColor(Color.DKGRAY);p.setTextSize(Math.min(11,slot/5));String t=labels.get(i);if(t.length()>5)t=t.substring(t.length()-5);c.drawText(t,left-2,bottom+17,p);}}
 }

 public void showLibrary(){
  String[] names={s("lib_squat"),s("lib_pushup"),s("lib_lunge"),s("lib_plank"),s("lib_dumbbell_row"),s("lib_walk")};
  String[] details={s("detail_squat"),s("detail_pushup"),s("detail_lunge"),s("detail_plank"),s("detail_row"),s("detail_walk")};
  String[] gifs={"squat.gif","pushup.gif","lunge.gif","plank.gif","row.gif","walk.gif"};
  new AlertDialog.Builder(a).setTitle(s("exercise_library")).setItems(names,(d,which)->{
   LinearLayout l=column();ExerciseGifView image=new ExerciseGifView(a,gifs[which]);image.setContentDescription(names[which]);int imageHeight=(int)(180*a.getResources().getDisplayMetrics().density);l.addView(image,new LinearLayout.LayoutParams(-1,imageHeight));
   TextView title=new TextView(a);title.setText(names[which]);title.setTextSize(20);l.addView(title);TextView body=new TextView(a);body.setText(details[which]);l.addView(body);
   new AlertDialog.Builder(a).setTitle(s("exercise_library")).setView(l).setPositiveButton(android.R.string.ok,null).show();
  }).setNegativeButton(android.R.string.cancel,null).show();
 }

 public void buildPersonalPlan(){
  LinearLayout l=column();
  Spinner goal=spinner(new String[]{s("goal_strength"),s("goal_muscle"),s("goal_general"),s("goal_weight")});
  Spinner level=spinner(new String[]{s("level_beginner"),s("level_intermediate"),s("level_advanced")});
  Spinner equipment=spinner(new String[]{s("equipment_none"),s("equipment_home"),s("equipment_gym")});
  EditText days=field(s("days_per_week"),"3",android.text.InputType.TYPE_CLASS_NUMBER);
  EditText minutes=field(s("session_minutes"),"35",android.text.InputType.TYPE_CLASS_NUMBER);
  l.addView(label(s("training_goal")));l.addView(goal);l.addView(label(s("fitness_level")));l.addView(level);l.addView(label(s("available_equipment")));l.addView(equipment);l.addView(days);l.addView(minutes);
  new AlertDialog.Builder(a).setTitle(s("personal_plan_builder")).setView(l).setPositiveButton(s("create_plan"),(d,w)->{
   try{
    int n=parseLocalizedInt(days.getText().toString()),mins=parseLocalizedInt(minutes.getText().toString());if(n<1||n>7||mins<10||mins>180)throw new IllegalArgumentException();
    String planName=s("personal_plan_name")+" - "+goal.getSelectedItem();
    long id=db.addPlan(planName);String eq=(String)equipment.getSelectedItem();
    String[][] exercises;
    if(eq.equals(s("equipment_none")))exercises=new String[][]{{s("lib_squat"),"3","10"},{s("lib_pushup"),"3","8"},{s("lib_lunge"),"2","10"},{s("lib_plank"),"3","30"}};
    else if(eq.equals(s("equipment_home")))exercises=new String[][]{{s("lib_squat"),"3","12"},{s("lib_pushup"),"3","10"},{s("lib_dumbbell_row"),"3","10"},{s("lib_lunge"),"3","10"},{s("lib_plank"),"3","30"}};
    else exercises=new String[][]{{s("lib_squat"),"4","8"},{s("lib_pushup"),"3","10"},{s("lib_dumbbell_row"),"4","10"},{s("lib_lunge"),"3","10"},{s("lib_plank"),"3","40"}};
    int goalIndex=goal.getSelectedItemPosition();
    if(mins<=20&&exercises.length>3)exercises=Arrays.copyOf(exercises,3);
    int scale=level.getSelectedItemPosition()==0?0:(level.getSelectedItemPosition()==1?1:2);
    for(String[] ex:exercises){int sets=Integer.parseInt(ex[1])+scale;int reps=Integer.parseInt(ex[2]);if(goalIndex==0)reps=Math.min(reps,8);else if(goalIndex==1)reps=Math.max(reps,10);else if(goalIndex==3)reps=Math.max(reps,12);db.addExercise(id,ex[0],sets,reps);}
    a.getSharedPreferences("fityar_preferences",Context.MODE_PRIVATE).edit().putInt("weekly_goal",n).putInt("session_minutes",mins).putString("fitness_goal",String.valueOf(goal.getSelectedItem())).putInt("fitness_level",level.getSelectedItemPosition()).putString("equipment",eq).apply();
    message(s("plan_created"),s("plan_created_detail")+"\n"+s("weekly_target")+": "+n+"\n"+s("session_minutes")+": "+mins);
   }catch(Exception ex){message(s("error"),s("invalid_plan_values"));}
  }).setNegativeButton(android.R.string.cancel,null).show();
 }
 private Spinner spinner(String[] values){Spinner sp=new Spinner(a);sp.setAdapter(new ArrayAdapter<>(a,android.R.layout.simple_spinner_dropdown_item,values));return sp;}
 private TextView label(String t){TextView v=new TextView(a);v.setText(t);v.setTextSize(15);return v;}


 /** Goal-specific exercise and nutrition guide based on age, height, weight and activity. */
 public void buildGoalPlan(){
  LinearLayout l=column();
  Spinner goal=spinner(new String[]{tg("Weight loss","کاهش وزن"),tg("Fitness & body composition","فیتنس و تناسب اندام"),tg("Muscle building","عضله‌سازی")});
  Spinner region=spinner(new String[]{tg("Full body","کل بدن"),tg("Shoulders","سرشانه"),tg("Arms","بازو"),tg("Abs / core","شکم و میان‌تنه"),tg("Legs & glutes","پا و باسن"),tg("Chest","سینه"),tg("Back","پشت و زیربغل")});
  Spinner sex=spinner(new String[]{tg("Male","مرد"),tg("Female","زن")});
  EditText age=field(tg("Age (years)","سن (سال)"),"30",android.text.InputType.TYPE_CLASS_NUMBER);
  EditText height=field(tg("Height (cm)","قد (سانتی‌متر)"),"170",android.text.InputType.TYPE_CLASS_NUMBER|android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
  EditText weight=field(tg("Weight (kg)","وزن (کیلوگرم)"),"70",android.text.InputType.TYPE_CLASS_NUMBER|android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
  Spinner activity=spinner(new String[]{tg("Low activity","فعالیت کم"),tg("Moderate activity","فعالیت متوسط"),tg("High activity","فعالیت زیاد")});
  l.addView(label(tg("Choose your goal","هدف اصلی")));l.addView(goal);
  l.addView(label(tg("Muscle-building target area","ناحیهٔ هدف عضله‌سازی")));l.addView(region);
  l.addView(label(tg("Sex for calorie estimate","جنسیت برای تخمین کالری")));l.addView(sex);
  l.addView(age);l.addView(height);l.addView(weight);l.addView(label(tg("Usual daily activity","فعالیت معمول روزانه")));l.addView(activity);
  new AlertDialog.Builder(a).setTitle(tg("Personal training & nutrition","تمرین و تغذیهٔ شخصی")).setView(l).setPositiveButton(tg("Create guide","ساخت راهنما"),(d,w)->{
   try{
    int years=parseLocalizedInt(age.getText().toString());
    double cm=parseLocalizedDouble(height.getText().toString()),kg=parseLocalizedDouble(weight.getText().toString());
    if(years<18||years>100||cm<120||cm>230||kg<30||kg>300)throw new IllegalArgumentException();
    int g=goal.getSelectedItemPosition(),r=region.getSelectedItemPosition(),sx=sex.getSelectedItemPosition(),act=activity.getSelectedItemPosition();
    double bmi=kg/((cm/100.0)*(cm/100.0));
    double bmr=10*kg+6.25*cm-5*years+(sx==0?5:-161);
    double maintenance=bmr*(act==0?1.35:act==1?1.55:1.725);
    double target;if(g==0)target=bmi<18.5?maintenance:Math.max(1200,maintenance-350);else target=g==1?maintenance:maintenance+200;target=Math.max(1200,target);
    double protein=(g==2?1.8:1.5)*kg;
    StringBuilder out=new StringBuilder();
    out.append(tg("Profile","مشخصات")).append("\n").append(tg("BMI (screening estimate only)","BMI (فقط شاخص غربالگری تقریبی)")).append(": ").append(String.format(Locale.getDefault(),"%.1f",bmi)).append("\n")
      .append(tg("Estimated maintenance","کالری نگهدارندهٔ تخمینی")).append(": ").append(Math.round(maintenance)).append(" kcal/day\n")
      .append(tg("Starting energy target","هدف اولیهٔ انرژی")).append(": ").append(Math.round(target)).append(" kcal/day\n")
      .append(tg("Protein guide","راهنمای پروتئین")).append(": ").append(Math.round(protein)).append(" g/day\n\n");
    if(g==0&&bmi<18.5)out.append(tg("Weight note: BMI is below 18.5, so this guide does not set a calorie deficit. Consider discussing unintended weight loss or nutrition concerns with a qualified clinician.","نکتهٔ وزن: BMI کمتر از ۱۸٫۵ است؛ بنابراین این راهنما کسری کالری پیشنهاد نمی‌کند. اگر کاهش وزن ناخواسته یا نگرانی تغذیه‌ای دارید، با متخصص سلامت مشورت کنید.")).append("\n\n");
    if(g==0){
     out.append(tg("TRAINING — WEIGHT LOSS","تمرین — کاهش وزن")).append("\n").append(tg("3–5 days/week: 25–45 min brisk walking/cycling plus 2–3 full-body strength sessions. Keep the calorie deficit modest and increase activity gradually.","۳ تا ۵ روز در هفته: ۲۵ تا ۴۵ دقیقه پیاده‌روی تند یا دوچرخه، به‌علاوهٔ ۲ تا ۳ جلسه قدرتی کل بدن. کسری کالری ملایم داشته باشید و فعالیت را تدریجی افزایش دهید.")).append("\n\n")
      .append(tg("FOOD — vegetables, fruit, beans, whole grains and lean protein; reduce sugary drinks and highly processed snacks. Aim for slow, sustainable loss.","تغذیه — سبزی، میوه، حبوبات، غلات کامل و پروتئین کم‌چرب را بیشتر کنید؛ نوشیدنی شیرین و تنقلات بسیار فرآوری‌شده را کاهش دهید. کاهش وزن آهسته و پایدار را هدف بگیرید."));
    }else if(g==1){
     out.append(tg("TRAINING — FITNESS","تمرین — فیتنس و تناسب اندام")).append("\n").append(tg("3 strength sessions/week for all major muscle groups, 2 moderate cardio sessions, and mobility work. Start with 2–3 sets of 8–15 controlled reps.","۳ جلسه قدرتی در هفته برای عضلات اصلی، ۲ جلسه هوازی متوسط و تمرین تحرک مفاصل. با ۲ تا ۳ ست و ۸ تا ۱۵ تکرار کنترل‌شده شروع کنید.")).append("\n\n")
      .append(tg("FOOD — start near maintenance; include protein at each meal, vegetables, whole-food carbohydrates and healthy fats. Adjust after 2–3 weeks based on progress.","تغذیه — نزدیک کالری نگهدارنده شروع کنید؛ در هر وعده پروتئین، سبزیجات، کربوهیدرات کامل و چربی مفید بگنجانید. پس از ۲ تا ۳ هفته براساس روند پیشرفت تنظیم کنید."));
    }else{
     String area;
     switch(r){
      case 1: area=tg("shoulders: overhead press, lateral raises, rear-delt raises","سرشانه: پرس سرشانه، نشر جانب، نشر خم");break;
      case 2: area=tg("arms: biceps curls, hammer curls, triceps extensions","بازو: جلو بازو، جلو بازو چکشی، پشت بازو");break;
      case 3: area=tg("abs/core: plank, dead bug, reverse crunch; visible abs also depend on overall body fat","شکم و میان‌تنه: پلانک، ددباگ، کرانچ معکوس؛ نمایان‌شدن شکم به چربی کل بدن هم وابسته است");break;
      case 4: area=tg("legs/glutes: squats, lunges, hip hinges, glute bridges","پا و باسن: اسکوات، لانج، هیپ‌هینج، پل باسن");break;
      case 5: area=tg("chest: push-ups, dumbbell/floor press","سینه: شنا، پرس دمبل یا پرس روی زمین");break;
      case 6: area=tg("back: rows, band rows, safe pulling movements","پشت و زیربغل: انواع پارویی، پارویی کشی و حرکات کششی ایمن");break;
      default: area=tg("full body: squat, push-up, row, hip hinge and plank","کل بدن: اسکوات، شنا، پارویی، هیپ‌هینج و پلانک");
     }
     out.append(tg("TRAINING — MUSCLE BUILDING","تمرین — عضله‌سازی")).append("\n").append(tg("Target area: ","ناحیهٔ هدف: ")).append(area).append("\n")
      .append(tg("Train the selected area twice weekly with about 48 hours between hard sessions. Begin with 3 exercises, 2–4 sets each and 6–15 reps; leave 1–3 good reps in reserve and increase reps/load gradually.","ناحیهٔ انتخابی را هفته‌ای ۲ بار تمرین دهید و بین جلسات سنگین حدود ۴۸ ساعت فاصله بگذارید. با ۳ حرکت، هرکدام ۲ تا ۴ ست و ۶ تا ۱۵ تکرار شروع کنید؛ ۱ تا ۳ تکرار توان اضافه باقی بگذارید و تکرار/وزنه را تدریجی افزایش دهید.")).append("\n\n")
      .append(tg("FOOD — start near maintenance or a small surplus; spread protein across 3–4 meals, eat enough carbohydrates for training, and include vegetables, fruit and healthy fats. Review progress every 2–3 weeks.","تغذیه — نزدیک کالری نگهدارنده یا با مازاد اندک شروع کنید؛ پروتئین را در ۳ تا ۴ وعده تقسیم کنید، کربوهیدرات کافی برای تمرین بخورید و سبزی، میوه و چربی مفید مصرف کنید. هر ۲ تا ۳ هفته پیشرفت را بررسی کنید."));
    }
    out.append("\n\n").append(tg("Safety: these are rough estimates, not medical advice. The formula does not account for illness, medication, pregnancy or eating-disorder history. This guide is for adults; seek professional advice if needed.","ایمنی: این اعداد تخمینی‌اند و توصیهٔ پزشکی نیستند. بیماری، دارو، بارداری یا سابقهٔ اختلال خوردن در فرمول لحاظ نشده‌اند. این راهنما برای بزرگسالان است؛ در صورت نیاز با متخصص مشورت کنید."));
    new AlertDialog.Builder(a).setTitle(tg("Your training & food guide","راهنمای تمرین و تغذیهٔ شما")).setMessage(out.toString()).setPositiveButton(android.R.string.ok,null).show();
   }catch(Exception ex){message(tg("Check your details","اطلاعات را بررسی کنید"),tg("Enter age 18–100, height 120–230 cm, and weight 30–300 kg.","سن ۱۸ تا ۱۰۰، قد ۱۲۰ تا ۲۳۰ سانتی‌متر و وزن ۳۰ تا ۳۰۰ کیلوگرم وارد کنید."));}
  }).setNegativeButton(android.R.string.cancel,null).show();
 }
 private String tg(String en,String fa){return a.getSharedPreferences("fit_settings",Context.MODE_PRIVATE).getString("language","fa").equals("en")?en:fa;}

 public void showCoach(){
  int goal=a.getSharedPreferences("fityar_preferences",Context.MODE_PRIVATE).getInt("weekly_goal",3);
  int days=db.weeklyTrainingDays();
  String advice;
  if(days==0)advice=s("coach_start");
  else if(days<goal)advice=s("coach_consistency")+" "+(goal-days)+" "+s("sessions_left");
  else advice=s("coach_progress");
  try(Cursor c=db.recentLogs()){if(c.moveToFirst()){int rpe=c.getInt(5);if(rpe>=9)advice+="\n\n"+s("coach_recovery");else if(rpe<=5)advice+="\n\n"+s("coach_maybe_progress");}}
  message(s("smart_coach"),advice+"\n\n"+s("coach_safety"));
 }

 public void showWeeklyGoal(){
  int current=db.weeklyTrainingDays();
  int goal=a.getSharedPreferences("fityar_preferences",Context.MODE_PRIVATE).getInt("weekly_goal",3);
  EditText e=field(s("weekly_goal_hint"),String.valueOf(goal),android.text.InputType.TYPE_CLASS_NUMBER);
  new AlertDialog.Builder(a).setTitle(s("weekly_goal")).setMessage(s("weekly_progress")+": "+Math.min(current,goal)+"/"+goal).setView(e).setPositiveButton(s("save"),(d,w)->{try{int v=parseLocalizedInt(e.getText().toString());if(v<1||v>7)throw new Exception();a.getSharedPreferences("fityar_preferences",Context.MODE_PRIVATE).edit().putInt("weekly_goal",v).apply();message(s("saved"),s("weekly_goal_saved"));}catch(Exception ex){message(s("error"),s("invalid_plan_values"));}}).setNegativeButton(android.R.string.cancel,null).show();
 }

 public void showStepGoal(){int current=a.getSharedPreferences("fityar_preferences",0).getInt("step_goal",5000);EditText e=field(s("step_goal_hint"),String.valueOf(current),android.text.InputType.TYPE_CLASS_NUMBER);new AlertDialog.Builder(a).setTitle(s("step_goal")).setMessage(s("step_goal_session_note")).setView(e).setPositiveButton(s("save"),(d,w)->{try{int v=parseLocalizedInt(e.getText().toString());if(v<100||v>100000)throw new Exception();a.getSharedPreferences("fityar_preferences",0).edit().putInt("step_goal",v).apply();if(a instanceof MainActivity)((MainActivity)a).updateStepsCount();message(s("saved"),s("step_goal_saved"));}catch(Exception ex){message(s("error"),s("invalid_plan_values"));}}).setNegativeButton(android.R.string.cancel,null).show();}

 public void showReminders(){
  LinearLayout l=column();CheckBox enabled=new CheckBox(a);enabled.setText(s("enable_reminder"));enabled.setChecked(a.getSharedPreferences("fityar_preferences",0).getBoolean("reminder_enabled",false));TimePicker time=new TimePicker(a);time.setIs24HourView(true);time.setHour(a.getSharedPreferences("fityar_preferences",0).getInt("reminder_hour",19));time.setMinute(a.getSharedPreferences("fityar_preferences",0).getInt("reminder_minute",0));l.addView(enabled);l.addView(label(s("reminder_time")));l.addView(time);
  new AlertDialog.Builder(a).setTitle(s("workout_reminders")).setView(l).setPositiveButton(s("save"),(d,w)->{Context c=a.getApplicationContext();c.getSharedPreferences("fityar_preferences",0).edit().putBoolean("reminder_enabled",enabled.isChecked()).putInt("reminder_hour",time.getHour()).putInt("reminder_minute",time.getMinute()).apply();if(enabled.isChecked()){if(Build.VERSION.SDK_INT>=33&&a.checkSelfPermission("android.permission.POST_NOTIFICATIONS")!=android.content.pm.PackageManager.PERMISSION_GRANTED){if(a instanceof MainActivity){((MainActivity)a).requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"},NOTIFY);}return;}WorkoutReminderReceiver.schedule(c,time.getHour(),time.getMinute());}else WorkoutReminderReceiver.cancel(c);message(s("saved"),enabled.isChecked()?s("reminder_set"):s("reminder_disabled"));}).setNegativeButton(android.R.string.cancel,null).show();
 }

 public void onRequestPermissionsResult(int requestCode,int[] grantResults){if(requestCode==NOTIFY){if(grantResults.length>0&&grantResults[0]==android.content.pm.PackageManager.PERMISSION_GRANTED){Context c=a.getApplicationContext();if(c.getSharedPreferences("fityar_preferences",0).getBoolean("reminder_enabled",false))WorkoutReminderReceiver.schedule(c,c.getSharedPreferences("fityar_preferences",0).getInt("reminder_hour",19),c.getSharedPreferences("fityar_preferences",0).getInt("reminder_minute",0));}else message(s("permission_required"),s("notification_permission_message"));}}

 public void backup(){
  String[] categories={tr("Workout plans & logs","برنامه‌ها و سوابق تمرین"),tr("Health records","سوابق سلامت"),tr("Step counter history","سوابق قدم‌شمار"),tr("App preferences","تنظیمات برنامه")};
  boolean[] selected={true,true,true,true};
  new AlertDialog.Builder(a).setTitle(tr("Choose backup sections","انتخاب بخش‌های پشتیبان")).setMessage(tr("Backup files may contain sensitive health and personal data and are not encrypted. Store them securely.","فایل پشتیبان ممکن است شامل اطلاعات شخصی و سلامت باشد و رمزگذاری نمی‌شود؛ آن را در محل امن نگه دارید.")).setMultiChoiceItems(categories,selected,(d,which,checked)->selected[which]=checked)
   .setPositiveButton(tr("Continue","ادامه"),(d,w)->{
    boolean any=false;for(boolean value:selected)any|=value;
    if(!any){message(tr("Error","خطا"),tr("Select at least one section.","حداقل یک بخش را انتخاب کنید."));return;}
    Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("application/json");i.putExtra(Intent.EXTRA_TITLE,"fityar-backup.json");
    pendingBackupSections=selected.clone();a.startActivityForResult(i,BACKUP);
   }).setNegativeButton(android.R.string.cancel,null).show();
 }
 private boolean[] pendingBackupSections={true,true,true,true};
 private JSONObject createSelectiveBackup(boolean[] selected)throws Exception{
  JSONObject root=new JSONObject();root.put("format","fityar-selective-backup");root.put("version",1);root.put("createdAt",System.currentTimeMillis());
  JSONArray sections=new JSONArray();
  if(selected[0]){JSONObject section=new JSONObject();section.put("id","workouts");section.put("plans",tableToJson("plans"));section.put("exercises",tableToJson("exercises"));section.put("logs",tableToJson("logs"));sections.put(section);}
  if(selected[1]){JSONObject section=new JSONObject();section.put("id","health");section.put("sleep_logs",tableToJson("sleep_logs"));section.put("vital_logs",tableToJson("vital_logs"));section.put("body_metrics",tableToJson("body_metrics"));sections.put(section);}
  if(selected[2]){JSONObject section=new JSONObject();section.put("id","steps");section.put("step_counter",preferencesToJson("step_counter",new HashSet<>(Arrays.asList("running","paused"))));sections.put(section);}
  if(selected[3]){JSONObject section=new JSONObject();section.put("id","preferences");section.put("fityar_preferences",preferencesToJson("fityar_preferences"));section.put("fit_settings",preferencesToJson("fit_settings"));sections.put(section);}
  root.put("sections",sections);return root;
 }
 private JSONArray tableToJson(String table)throws Exception{
  JSONArray rows=new JSONArray();
  try(Cursor cursor=db.getReadableDatabase().query(table,null,null,null,null,null,null)){
   String[] columns=cursor.getColumnNames();
   while(cursor.moveToNext()){
    JSONObject row=new JSONObject();
    for(int i=0;i<columns.length;i++){
     switch(cursor.getType(i)){
      case Cursor.FIELD_TYPE_NULL:row.put(columns[i],JSONObject.NULL);break;
      case Cursor.FIELD_TYPE_INTEGER:row.put(columns[i],cursor.getLong(i));break;
      case Cursor.FIELD_TYPE_FLOAT:row.put(columns[i],cursor.getDouble(i));break;
      case Cursor.FIELD_TYPE_BLOB:row.put(columns[i],android.util.Base64.encodeToString(cursor.getBlob(i),android.util.Base64.NO_WRAP));break;
      default:row.put(columns[i],cursor.getString(i));
     }
    }
    rows.put(row);
   }
  }
  return rows;
 }
 private JSONObject preferencesToJson(String name)throws Exception{return preferencesToJson(name,Collections.emptySet());}
 private JSONObject preferencesToJson(String name,Set<String> excluded)throws Exception{
  JSONObject out=new JSONObject();Map<String,?> values=a.getSharedPreferences(name,Context.MODE_PRIVATE).getAll();
  for(Map.Entry<String,?> entry:values.entrySet()){
   if(excluded.contains(entry.getKey()))continue;
   Object value=entry.getValue();
   if(value instanceof Boolean||value instanceof Integer||value instanceof Long||value instanceof Float||value instanceof String)out.put(entry.getKey(),value);
   else if(value instanceof Set){JSONArray array=new JSONArray();for(Object item:(Set<?>)value)array.put(String.valueOf(item));out.put(entry.getKey(),array);}
  }
  return out;
 }
 public void restore(){
  Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("application/json");i.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"application/json","text/plain","*/*"});a.startActivityForResult(i,RESTORE);
 }
 public boolean onActivityResult(int requestCode,int resultCode,Intent data){
  if(requestCode!=BACKUP&&requestCode!=RESTORE)return false;
  if(resultCode!=Activity.RESULT_OK||data==null||data.getData()==null)return true;
  try{
   if(requestCode==BACKUP){
    JSONObject backup=createSelectiveBackup(pendingBackupSections);
    try(OutputStream out=a.getContentResolver().openOutputStream(data.getData())){
     if(out==null)throw new IOException("No output stream");out.write(backup.toString(2).getBytes(java.nio.charset.StandardCharsets.UTF_8));out.flush();
    }
    message(tr("Backup complete","پشتیبان‌گیری کامل شد"),tr("Selected sections were saved.","بخش‌های انتخاب‌شده ذخیره شدند."));
    return true;
   }
   byte[] imported;
   try(InputStream in=a.getContentResolver().openInputStream(data.getData())){
    if(in==null)throw new IOException("No input stream");imported=readBackupBytes(in);
   }
   int first=0;while(first<imported.length&&Character.isWhitespace((char)(imported[first]&0xff)))first++;
   if(first>=imported.length)throw new IOException("Empty backup");
   if(imported[first]!='{'){
    restoreLegacyDatabase(imported);
    return true;
   }
   JSONObject backup=new JSONObject(new String(imported,java.nio.charset.StandardCharsets.UTF_8));
   if(!"fityar-selective-backup".equals(backup.optString("format"))||backup.optInt("version")!=1||!(backup.opt("sections") instanceof JSONArray))throw new IOException("Unsupported backup format");
   JSONArray sections=backup.getJSONArray("sections");
   ArrayList<JSONObject> valid=new ArrayList<>();
   ArrayList<String> labels=new ArrayList<>();
   ArrayList<String> ids=new ArrayList<>();
   for(int i=0;i<sections.length();i++){
    JSONObject section=sections.optJSONObject(i);if(section==null)continue;String id=section.optString("id","");
    if("workouts".equals(id)&&section.opt("plans") instanceof JSONArray&&section.opt("exercises") instanceof JSONArray&&section.opt("logs") instanceof JSONArray){valid.add(section);ids.add(id);labels.add(tr("Workout plans & logs","برنامه‌ها و سوابق تمرین"));}
    else if("health".equals(id)&&section.opt("sleep_logs") instanceof JSONArray&&section.opt("vital_logs") instanceof JSONArray&&section.opt("body_metrics") instanceof JSONArray){valid.add(section);ids.add(id);labels.add(tr("Health records","سوابق سلامت"));}
    else if("steps".equals(id)&&section.opt("step_counter") instanceof JSONObject){valid.add(section);ids.add(id);labels.add(tr("Step counter history","سوابق قدم‌شمار"));}
    else if("preferences".equals(id)&&section.opt("fityar_preferences") instanceof JSONObject&&section.opt("fit_settings") instanceof JSONObject){valid.add(section);ids.add(id);labels.add(tr("App preferences","تنظیمات برنامه"));}
   }
   if(valid.isEmpty())throw new IOException("No supported backup sections");
   boolean[] chosen=new boolean[valid.size()];Arrays.fill(chosen,true);
   new AlertDialog.Builder(a).setTitle(tr("Choose sections to restore","انتخاب بخش‌های بازیابی")).setMultiChoiceItems(labels.toArray(new String[0]),chosen,(dialog,which,checked)->chosen[which]=checked)
    .setPositiveButton(tr("Restore selected","بازیابی بخش‌های انتخاب‌شده"),(dialog,which)->{
     boolean any=false;for(boolean value:chosen)any|=value;
     if(!any){message(tr("Error","خطا"),tr("Select at least one section.","حداقل یک بخش را انتخاب کنید."));return;}
     new AlertDialog.Builder(a).setTitle(tr("Confirm restore","تأیید بازیابی")).setMessage(tr("Selected sections will replace their current data. Other sections will remain unchanged.","بخش‌های انتخاب‌شده جایگزین اطلاعات فعلی همان بخش می‌شوند؛ سایر بخش‌ها تغییر نمی‌کنند."))
      .setPositiveButton(tr("Restore","بازیابی"),(confirm,button)->{
       try{db.getWritableDatabase().beginTransaction();
        try{
         for(int i=0;i<valid.size();i++)if(chosen[i]&&!"preferences".equals(ids.get(i)))restoreSection(valid.get(i),ids.get(i));
         db.getWritableDatabase().setTransactionSuccessful();
        }finally{db.getWritableDatabase().endTransaction();}
        for(int i=0;i<valid.size();i++)if(chosen[i]&&"steps".equals(ids.get(i))){
         Intent finish=new Intent(a,StepCounterService.class);finish.setAction("FINISH");a.startService(finish);
         restorePreferences("step_counter",valid.get(i).getJSONObject("step_counter"));
        }
        for(int i=0;i<valid.size();i++)if(chosen[i]&&"preferences".equals(ids.get(i))){
         JSONObject section=valid.get(i);
         restorePreferences("fityar_preferences",section.getJSONObject("fityar_preferences"));
         restorePreferences("fit_settings",section.getJSONObject("fit_settings"));
         Context app=a.getApplicationContext();android.content.SharedPreferences prefs=app.getSharedPreferences("fityar_preferences",Context.MODE_PRIVATE);
         if(prefs.getBoolean("reminder_enabled",false))WorkoutReminderReceiver.schedule(app,prefs.getInt("reminder_hour",19),prefs.getInt("reminder_minute",0));
         else WorkoutReminderReceiver.cancel(app);
        }
        message(tr("Restore complete","بازیابی کامل شد"),tr("Selected sections were restored. Other sections were left unchanged.","بخش‌های انتخاب‌شده بازیابی شدند و سایر بخش‌ها بدون تغییر ماندند."));
       }catch(Exception ex){message(tr("Error","خطا"),tr("Could not restore backup. Current data was kept where possible.","بازیابی انجام نشد؛ تا حد امکان اطلاعات فعلی حفظ شدند.")+"\n"+ex.getMessage());}
      }).setNegativeButton(android.R.string.cancel,null).show();
    }).setNegativeButton(android.R.string.cancel,null).show();
  }catch(Exception ex){message(tr("Error","خطا"),tr("Invalid or unreadable backup file.","فایل پشتیبان نامعتبر است یا خوانده نمی‌شود.")+"\n"+ex.getMessage());}
  return true;
 }
 private void restoreLegacyDatabase(byte[] imported)throws Exception{
  File dbFile=a.getDatabasePath(WorkoutDb.DATABASE_NAME);
  File tmp=new File(a.getCacheDir(),"fit-yar-legacy-restore.tmp");
  File rollback=new File(a.getCacheDir(),"fit-yar-legacy-rollback.db");
  try{
   try(OutputStream out=new FileOutputStream(tmp)){out.write(imported);out.flush();}
   validateDatabase(tmp);
   db.close();
   if(rollback.exists()&&!rollback.delete())throw new IOException("Cannot clear rollback file");
   if(dbFile.exists())try(InputStream in=new FileInputStream(dbFile);OutputStream out=new FileOutputStream(rollback)){copy(in,out);}
   deleteSidecars(dbFile);
   if(dbFile.exists()&&!dbFile.delete())throw new IOException("Cannot replace database");
   try(InputStream in=new FileInputStream(tmp);OutputStream out=new FileOutputStream(dbFile)){copy(in,out);}
   validateDatabase(dbFile);
   db.getReadableDatabase();
   rollback.delete();
   message(tr("Restore complete","بازیابی کامل شد"),tr("Legacy full-database backup restored.","پشتیبان قدیمیِ کل پایگاه داده بازیابی شد."));
  }catch(Exception ex){
   try{
    db.close();deleteSidecars(dbFile);
    if(rollback.exists()){
     if(dbFile.exists())dbFile.delete();
     try(InputStream in=new FileInputStream(rollback);OutputStream out=new FileOutputStream(dbFile)){copy(in,out);}
    }
    db.getReadableDatabase();
   }catch(Exception ignored){}
   throw ex;
  }finally{if(tmp.exists())tmp.delete();}
 }
 private void validateDatabase(File file)throws IOException{
  if(!file.isFile()||file.length()<100)throw new IOException("Empty database");
  try(SQLiteDatabase check=SQLiteDatabase.openDatabase(file.getAbsolutePath(),null,SQLiteDatabase.OPEN_READONLY);
      Cursor integrity=check.rawQuery("PRAGMA integrity_check",null)){
   if(!integrity.moveToFirst()||!"ok".equalsIgnoreCase(integrity.getString(0)))throw new IOException("Invalid SQLite database");
   try(Cursor version=check.rawQuery("PRAGMA user_version",null)){
    if(!version.moveToFirst()||version.getInt(0)>4)throw new IOException("Backup comes from a newer app version");
   }
   try(Cursor tables=check.rawQuery("SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name IN ('plans','exercises','logs')",null)){
    if(!tables.moveToFirst()||tables.getInt(0)!=3)throw new IOException("Not a FitYar database");
   }
  }catch(android.database.SQLException ex){throw new IOException("Invalid FitYar backup",ex);}
 }
 private void deleteSidecars(File dbFile){
  for(String suffix:new String[]{"-wal","-shm","-journal"}){File sidecar=new File(dbFile.getAbsolutePath()+suffix);if(sidecar.exists())sidecar.delete();}
 }
 private void restoreSection(JSONObject section,String id)throws Exception{
  SQLiteDatabase database=db.getWritableDatabase();
  if("workouts".equals(id)){
   restoreTable(database,"plans",section.getJSONArray("plans"));restoreTable(database,"exercises",section.getJSONArray("exercises"));restoreTable(database,"logs",section.getJSONArray("logs"));
  }else if("health".equals(id)){
   restoreTable(database,"sleep_logs",section.getJSONArray("sleep_logs"));restoreTable(database,"vital_logs",section.getJSONArray("vital_logs"));restoreTable(database,"body_metrics",section.getJSONArray("body_metrics"));
  }else if("preferences".equals(id)){
   restorePreferences("fityar_preferences",section.getJSONObject("fityar_preferences"));restorePreferences("fit_settings",section.getJSONObject("fit_settings"));
  }else throw new IOException("Unknown section");
 }
 private void restoreTable(SQLiteDatabase database,String table,JSONArray rows)throws Exception{
  database.delete(table,null,null);
  java.util.HashSet<String> allowed=new java.util.HashSet<>();
  try(Cursor info=database.rawQuery("PRAGMA table_info("+table+")",null)){while(info.moveToNext())allowed.add(info.getString(1));}
  for(int i=0;i<rows.length();i++){
   JSONObject row=rows.getJSONObject(i);ContentValues values=new ContentValues();
   JSONArray names=row.names();if(names==null)continue;
   for(int j=0;j<names.length();j++){
    String column=names.getString(j);if(!allowed.contains(column))continue;Object value=row.get(column);
    if(value==JSONObject.NULL)values.putNull(column);
    else if(value instanceof Boolean)values.put(column,(Boolean)value);
    else if(value instanceof Integer)values.put(column,(Integer)value);
    else if(value instanceof Long)values.put(column,(Long)value);
    else if(value instanceof Number)values.put(column,((Number)value).doubleValue());
    else if(value instanceof String)values.put(column,(String)value);
    else throw new IOException("Unsupported value in "+table+"."+column);
   }
   database.insertOrThrow(table,null,values);
  }
 }
 private void restorePreferences(String name,JSONObject json){
  android.content.SharedPreferences.Editor editor=a.getSharedPreferences(name,Context.MODE_PRIVATE).edit().clear();
  JSONArray keys=json.names();if(keys!=null)for(int i=0;i<keys.length();i++){
   String key=keys.optString(i);Object value=json.opt(key);
   if(value==JSONObject.NULL)continue;
   if(value instanceof Boolean)editor.putBoolean(key,(Boolean)value);
   else if(value instanceof Integer)editor.putInt(key,(Integer)value);
   else if(value instanceof Long)editor.putLong(key,(Long)value);
   else if(value instanceof Number)editor.putFloat(key,((Number)value).floatValue());
   else if(value instanceof String)editor.putString(key,(String)value);
   else if(value instanceof JSONArray){HashSet<String> set=new HashSet<>();JSONArray array=(JSONArray)value;for(int j=0;j<array.length();j++)set.add(array.optString(j));editor.putStringSet(key,set);}
  }
  editor.apply();
 }
 private byte[] readBackupBytes(InputStream in)throws IOException{
  final int maxBytes=25*1024*1024;ByteArrayOutputStream bytes=new ByteArrayOutputStream();byte[] buffer=new byte[8192];int count,total=0;
  while((count=in.read(buffer))!=-1){total+=count;if(total>maxBytes)throw new IOException("Backup exceeds the 25 MiB safety limit");bytes.write(buffer,0,count);}
  return bytes.toByteArray();
 }
 private void copy(InputStream in,OutputStream out)throws IOException{
  byte[] buffer=new byte[8192];int count;while((count=in.read(buffer))!=-1)out.write(buffer,0,count);out.flush();
 }
}
