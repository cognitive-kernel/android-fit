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

public final class AdvancedFeatures {
 private final Activity a; private final WorkoutDb db;
 private static final int BACKUP=4101, RESTORE=4102, NOTIFY=4103;
 public AdvancedFeatures(Activity activity, WorkoutDb database){a=activity;db=database;}
 private String s(String key){int id=a.getResources().getIdentifier(key,"string",a.getPackageName());return id==0?key:a.getString(id);}
 private EditText field(String hint,String value,int type){EditText e=new EditText(a);e.setHint(hint);if(value!=null)e.setText(value);e.setInputType(type);return e;}
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
  int[] images={R.drawable.exercise_squat,R.drawable.exercise_pushup,R.drawable.exercise_lunge,R.drawable.exercise_plank,R.drawable.exercise_row,R.drawable.exercise_walk};
  new AlertDialog.Builder(a).setTitle(s("exercise_library")).setItems(names,(d,which)->{
   LinearLayout l=column();ImageView image=new ImageView(a);image.setImageResource(images[which]);image.setContentDescription(names[which]);image.setAdjustViewBounds(true);image.setMaxHeight((int)(180*a.getResources().getDisplayMetrics().density));l.addView(image,new LinearLayout.LayoutParams(-1,(int)(160*a.getResources().getDisplayMetrics().density)));
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
    int n=Integer.parseInt(days.getText().toString()),mins=Integer.parseInt(minutes.getText().toString());if(n<1||n>7||mins<10||mins>180)throw new IllegalArgumentException();
    String planName=s("personal_plan_name")+" - "+goal.getSelectedItem();
    long id=db.addPlan(planName);String eq=(String)equipment.getSelectedItem();
    String[][] exercises;
    if(eq.equals(s("equipment_none")))exercises=new String[][]{{s("lib_squat"),"3","10"},{s("lib_pushup"),"3","8"},{s("lib_lunge"),"2","10"},{s("lib_plank"),"3","30"}};
    else if(eq.equals(s("equipment_home")))exercises=new String[][]{{s("lib_squat"),"3","12"},{s("lib_pushup"),"3","10"},{s("lib_dumbbell_row"),"3","10"},{s("lib_lunge"),"3","10"},{s("lib_plank"),"3","30"}};
    else exercises=new String[][]{{s("lib_squat"),"4","8"},{s("lib_pushup"),"3","10"},{s("lib_dumbbell_row"),"4","10"},{s("lib_lunge"),"3","10"},{s("lib_plank"),"3","40"}};
    int scale=level.getSelectedItemPosition()==0?0:(level.getSelectedItemPosition()==1?1:2);
    for(String[] ex:exercises){int sets=Integer.parseInt(ex[1])+scale;db.addExercise(id,ex[0],sets,Integer.parseInt(ex[2]));}
    a.getSharedPreferences("fityar_preferences",Context.MODE_PRIVATE).edit().putInt("weekly_goal",n).putInt("session_minutes",mins).putString("fitness_goal",String.valueOf(goal.getSelectedItem())).putInt("fitness_level",level.getSelectedItemPosition()).putString("equipment",eq).apply();
    message(s("plan_created"),s("plan_created_detail")+"\n"+s("weekly_target")+": "+n+"\n"+s("session_minutes")+": "+mins);
   }catch(Exception ex){message(s("error"),s("invalid_plan_values"));}
  }).setNegativeButton(android.R.string.cancel,null).show();
 }
 private Spinner spinner(String[] values){Spinner sp=new Spinner(a);sp.setAdapter(new ArrayAdapter<>(a,android.R.layout.simple_spinner_dropdown_item,values));return sp;}
 private TextView label(String t){TextView v=new TextView(a);v.setText(t);v.setTextSize(15);return v;}

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
  new AlertDialog.Builder(a).setTitle(s("weekly_goal")).setMessage(s("weekly_progress")+": "+Math.min(current,goal)+"/"+goal).setView(e).setPositiveButton(s("save"),(d,w)->{try{int v=Integer.parseInt(e.getText().toString());if(v<1||v>7)throw new Exception();a.getSharedPreferences("fityar_preferences",Context.MODE_PRIVATE).edit().putInt("weekly_goal",v).apply();message(s("saved"),s("weekly_goal_saved"));}catch(Exception ex){message(s("error"),s("invalid_plan_values"));}}).setNegativeButton(android.R.string.cancel,null).show();
 }

 public void showReminders(){
  LinearLayout l=column();CheckBox enabled=new CheckBox(a);enabled.setText(s("enable_reminder"));enabled.setChecked(a.getSharedPreferences("fityar_preferences",0).getBoolean("reminder_enabled",false));TimePicker time=new TimePicker(a);time.setIs24HourView(true);time.setHour(a.getSharedPreferences("fityar_preferences",0).getInt("reminder_hour",19));time.setMinute(a.getSharedPreferences("fityar_preferences",0).getInt("reminder_minute",0));l.addView(enabled);l.addView(label(s("reminder_time")));l.addView(time);
  new AlertDialog.Builder(a).setTitle(s("workout_reminders")).setView(l).setPositiveButton(s("save"),(d,w)->{Context c=a.getApplicationContext();c.getSharedPreferences("fityar_preferences",0).edit().putBoolean("reminder_enabled",enabled.isChecked()).putInt("reminder_hour",time.getHour()).putInt("reminder_minute",time.getMinute()).apply();if(enabled.isChecked()){if(Build.VERSION.SDK_INT>=33&&a.checkSelfPermission("android.permission.POST_NOTIFICATIONS")!=android.content.pm.PackageManager.PERMISSION_GRANTED){if(a instanceof MainActivity){((MainActivity)a).requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"},NOTIFY);}return;}WorkoutReminderReceiver.schedule(c,time.getHour(),time.getMinute());}else WorkoutReminderReceiver.cancel(c);message(s("saved"),enabled.isChecked()?s("reminder_set"):s("reminder_disabled"));}).setNegativeButton(android.R.string.cancel,null).show();
 }

 public void onRequestPermissionsResult(int requestCode,int[] grantResults){if(requestCode==NOTIFY){if(grantResults.length>0&&grantResults[0]==android.content.pm.PackageManager.PERMISSION_GRANTED){Context c=a.getApplicationContext();if(c.getSharedPreferences("fityar_preferences",0).getBoolean("reminder_enabled",false))WorkoutReminderReceiver.schedule(c,c.getSharedPreferences("fityar_preferences",0).getInt("reminder_hour",19),c.getSharedPreferences("fityar_preferences",0).getInt("reminder_minute",0));}else message(s("permission_required"),s("notification_permission_message"));}}

 public void backup(){Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("application/octet-stream");i.putExtra(Intent.EXTRA_TITLE,"fityar-backup.db");a.startActivityForResult(i,BACKUP);}
 public void restore(){new AlertDialog.Builder(a).setTitle(s("restore_backup")).setMessage(s("restore_warning")).setPositiveButton(s("continue"),(d,w)->{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("*/*");a.startActivityForResult(i,RESTORE);}).setNegativeButton(android.R.string.cancel,null).show();}
 public boolean onActivityResult(int requestCode,int resultCode,Intent data){
  if((requestCode!=BACKUP&&requestCode!=RESTORE)||resultCode!=Activity.RESULT_OK||data==null||data.getData()==null)return false;
  try{
   File dbFile=a.getDatabasePath("fit_yar.db");db.close();
   if(requestCode==BACKUP){db.getReadableDatabase().close();try(InputStream in=new FileInputStream(dbFile);OutputStream out=a.getContentResolver().openOutputStream(data.getData())){if(out==null)throw new IOException("No output stream");copy(in,out);}db.getReadableDatabase();message(s("backup_complete"),s("backup_saved"));}
   else{File tmp=new File(a.getCacheDir(),"fit-yar-restore.tmp");try(InputStream in=a.getContentResolver().openInputStream(data.getData());OutputStream out=new FileOutputStream(tmp)){if(in==null)throw new IOException("No input stream");copy(in,out);}try(SQLiteDatabase check=SQLiteDatabase.openDatabase(tmp.getAbsolutePath(),null,SQLiteDatabase.OPEN_READONLY);Cursor integrity=check.rawQuery("PRAGMA integrity_check",null)){if(!integrity.moveToFirst()||!"ok".equalsIgnoreCase(integrity.getString(0)))throw new IOException("Invalid database");try(Cursor tables=check.rawQuery("SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name IN ('plans','exercises','logs')",null)){if(!tables.moveToFirst()||tables.getInt(0)!=3)throw new IOException("Not a FitYar database");}}if(!tmp.renameTo(dbFile)){try(InputStream in=new FileInputStream(tmp);OutputStream out=new FileOutputStream(dbFile)){copy(in,out);}tmp.delete();}db.getReadableDatabase();message(s("restore_complete"),s("restore_saved"));}
  }catch(Exception ex){try{db.getReadableDatabase();}catch(Exception ignored){}message(s("error"),s("backup_failed"));}return true;
 }
 private void copy(InputStream in,OutputStream out)throws IOException{byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);out.flush();}
}
