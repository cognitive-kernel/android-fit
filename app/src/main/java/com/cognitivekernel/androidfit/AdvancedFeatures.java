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
 private static final int MAX_BACKUP_BYTES=25*1024*1024;
 public AdvancedFeatures(Activity activity, WorkoutDb database){a=activity;db=database;}
 private String s(String key){int id=a.getResources().getIdentifier(key,"string",a.getPackageName());return id==0?key:a.getString(id);}
 private String selectedLanguage(){String language=a.getSharedPreferences("fit_settings",Context.MODE_PRIVATE).getString("language",null);if(language==null)language=a.getSharedPreferences("fityar_preferences",Context.MODE_PRIVATE).getString("language","fa");return "en".equalsIgnoreCase(language)?"en":"fa";}
 private String tr(String en,String fa){return "en".equals(selectedLanguage())?en:fa;}
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
  java.util.concurrent.Future<ArrayList<ExerciseLibraryRow>> future=ensureLibraryFuture(a.getApplicationContext());
  if(future.isDone()){
   try{showLoadedExerciseLibrary(future.get());}
   catch(Exception ex){message(s("error"),tr("Could not load the exercise library.","بارگذاری کتابخانه حرکات انجام نشد."));}
   return;
  }
  final ProgressDialog loading=new ProgressDialog(a);
  loading.setTitle(s("exercise_library"));
  loading.setMessage(tr("Preparing exercise library…","در حال آماده‌سازی کتابخانه حرکات…"));
  loading.setIndeterminate(true);
  loading.setCancelable(false);
  loading.show();
  final android.os.Handler main=new android.os.Handler(android.os.Looper.getMainLooper());
  LIBRARY_EXECUTOR.execute(()->{
   ArrayList<ExerciseLibraryRow> rows=null;String error=null;
   try{rows=future.get();}catch(Exception ex){error=ex.getClass().getSimpleName();}
   final ArrayList<ExerciseLibraryRow> result=rows;final String loadError=error;
   main.post(()->{
    if(a.isFinishing()||(Build.VERSION.SDK_INT>=17&&a.isDestroyed())){loading.dismiss();return;}
    loading.dismiss();
    if(loadError!=null){message(s("error"),tr("Could not load the exercise library.","بارگذاری کتابخانه حرکات انجام نشد."));return;}
    showLoadedExerciseLibrary(result==null?new ArrayList<>():result);
   });
  });
 }

 private static final java.util.concurrent.ExecutorService LIBRARY_EXECUTOR=java.util.concurrent.Executors.newSingleThreadExecutor();
 private static volatile java.util.concurrent.Future<ArrayList<ExerciseLibraryRow>> libraryFuture;

 public static void preloadLibrary(Context context){ensureLibraryFuture(context.getApplicationContext());}

 private static synchronized java.util.concurrent.Future<ArrayList<ExerciseLibraryRow>> ensureLibraryFuture(Context context){
  if(libraryFuture==null){
   final Context app=context.getApplicationContext();
   libraryFuture=LIBRARY_EXECUTOR.submit(()->loadExerciseLibrary(app));
  }
  return libraryFuture;
 }

 private static ArrayList<ExerciseLibraryRow> loadExerciseLibrary(Context context)throws IOException{
  ArrayList<String> files=new ArrayList<>();
  collectExerciseGifFiles(context.getAssets(),"exercises",files);
  ArrayList<ExerciseLibraryRow> rows=new ArrayList<>(files.size());
  java.util.regex.Pattern idPattern=java.util.regex.Pattern.compile("(?:_\\[?(\\d{3,8})\\]?|\\s+\\[?(\\d{3,8})\\]?)$");
  for(String assetPath:files){
   String file=assetPath.substring("exercises/".length());
   String title=file.substring(0,file.length()-4);
   java.util.regex.Matcher matcher=idPattern.matcher(title);
   String sourceId="";
   if(matcher.find()){
    sourceId=matcher.group(1)!=null?matcher.group(1):matcher.group(2);
    title=title.substring(0,matcher.start());
   }
   String english=title.replace('_',' ').replaceAll("\\s+"," ").trim();
   if(english.isEmpty())continue;
   rows.add(new ExerciseLibraryRow(english,persianExerciseName(english),file,sourceId));
  }
  Collections.sort(rows,(left,right)->left.englishName.compareToIgnoreCase(right.englishName));
  return rows;
 }

 private static void collectExerciseGifFiles(android.content.res.AssetManager assets,String directory,ArrayList<String> output)throws IOException{
  String[] entries=assets.list(directory);
  if(entries==null)return;
  for(String entry:entries){
   String path=directory+"/"+entry;
   if(entry.toLowerCase(Locale.ROOT).endsWith(".gif"))output.add(path);
   else if(!entry.contains("."))collectExerciseGifFiles(assets,path,output);
  }
 }

 private static final class ExerciseLibraryRow{
  final String englishName,file,sourceId,gender;
  private volatile String persianName,exerciseType,bodyPart,equipment;
  ExerciseLibraryRow(String englishName,String persianNameUnused,String file,String sourceId){
   this.englishName=englishName;this.file=file;this.sourceId=sourceId;
   String lower=englishName.toLowerCase(Locale.ROOT);
   this.gender=containsAny(lower,"(female)"," female","female ")? "Female":
    (containsAny(lower,"(male)"," male","male ")?"Male":"All");
  }
  String exerciseType(){
   String value=exerciseType;if(value==null){value=classifyExerciseType(englishName);exerciseType=value;}return value;
  }
  String bodyPart(){
   String value=bodyPart;if(value==null){value=classifyBodyPart(englishName);bodyPart=value;}return value;
  }
  String equipment(){
   String value=equipment;if(value==null){value=classifyEquipment(englishName);equipment=value;}return value;
  }
  String persianName(){
   String value=persianName;
   if(value==null){value=persianExerciseName(englishName);persianName=value;}
   return value;
  }
  String displayName(boolean english){return english?englishName:persianName();}
  boolean matchesSearch(String query){
   if(englishName.toLowerCase(Locale.ROOT).contains(query))return true;
   boolean persianQuery=false;
   for(int i=0;i<query.length();i++){char ch=query.charAt(i);if(ch>='\u0600'&&ch<='\u06FF'){persianQuery=true;break;}}
   return persianQuery&&persianName().toLowerCase(Locale.ROOT).contains(query);
  }
  @Override public String toString(){return englishName+" "+persianName();}
 }

 private static String classifyExerciseType(String name){
  String n=name.toLowerCase(Locale.ROOT);
  if(containsAny(n,"stretch","mobility","flexibility","warm-up","warm up","cool-down","cool down","prasarita","vajrasana","pose","yoga"))return "Stretching";
  if(containsAny(n,"sprint","jog","run ","running","jump rope","jumping jack","burpee","mountain climber","high knees","skipping","cardio","aerobic","cycling","bike","treadmill","shadow boxing"))return "Aerobic";
  return "Strength";
 }

 private static String classifyBodyPart(String name){
  String n=name.toLowerCase(Locale.ROOT);
  if(containsAny(n,"yoga","vajrasana","prasarita","pilates"))return containsAny(n,"pilates")?"Pilates":"Yoga";
  if(containsAny(n,"boxing","punch","jab ","hook punch"))return "Boxing";
  if(containsAny(n,"neck","cervical"))return "Neck";
  if(containsAny(n,"forearm","wrist","radial deviator","ulnar deviator"))return "Forearms";
  if(containsAny(n,"finger","hand grip","wrist circles","wrist roller","hand "))return "Hands";
  if(containsAny(n,"ankle","toe ","toes","foot","feet","plantar"))return "Feet";
  if(containsAny(n,"calf","calves","calf raise"))return "Calves";
  if(containsAny(n,"shoulder","deltoid","rotator cuff","lateral raise","front raise","rear delt"))return "Shoulders";
  if(containsAny(n,"triceps","biceps","upper arm","preacher curl","hammer curl","arm curl","pushdown","push down"))return "Upper Arms";
  if(containsAny(n,"chest","pectoral","pec deck","bench press","push-up","push up","pushup","chest fly"))return "Chest";
  if(containsAny(n,"lat ","lat pulldown","pulldown","pull-up","pull up","pullup","chin-up","chin up","deadlift","back extension","back row","seated row","bent over row","renegade row","face pull","shrug","trapezius","rhomboid"))return "Back";
  if(containsAny(n,"hip","glute","gluteal","butt","thrust","abductor","adductor","clamshell"))return "Hips";
  if(containsAny(n,"squat","lunge","leg press","leg extension","leg curl","hamstring","quadriceps","quad ","thigh","step-up","step up","split squat","leg raise","knee extension"))return "Thighs";
  if(containsAny(n,"abdominal","ab crunch","crunch","sit-up","sit up","plank","oblique","waist","russian twist","bicycle crunch","hollow body","wipers","woodchop","wood chop"))return "Waist";
  if(containsAny(n,"jump","hop","plyometric","box drill","cone drill","zigzag","zig zag","agility","sprint"))return "Plyometrics";
  if(containsAny(n,"full body","thruster","clean and press","burpee","turkish get-up","turkish get up"))return "Full body";
  if(containsAny(n,"snatch","clean and jerk","weightlifting","power clean","jerk"))return "Weightlifting";
  if(containsAny(n,"stretch","mobility","flexibility"))return "Stretching";
  if(containsAny(n,"cardio","running","run ","jog","cycling","bike","treadmill","jump rope","jumping jack","high knees"))return "Cardio";
  if(containsAny(n,"face","jaw"))return "Face";
  return "Full body";
 }

 private static String classifyEquipment(String name){
  String n=name.toLowerCase(Locale.ROOT);
  if(containsAny(n,"ez-bar","ez bar","ez-barbell"))return "EZ Barbell";
  if(containsAny(n,"smith machine","smith"))return "Smith machine";
  if(containsAny(n,"kettlebell","kettle bell"))return "Kettlebell";
  if(containsAny(n,"dumbbell","dumbell","db "))return "Dumbbell";
  if(containsAny(n,"barbell","barbell","olympic bar","straight bar","ez bar","ez-bar"))return "Barbell";
  if(containsAny(n,"trap bar","hex bar"))return "Trap bar";
  if(containsAny(n,"cable","pulley","pushdown","push down"))return "Cable";
  if(containsAny(n,"resistance band","theraband","mini band","loop band"))return "Resistance Band";
  if(containsAny(n,"band"))return "Band";
  if(containsAny(n,"medicine ball","med ball"))return "Medicine Ball";
  if(containsAny(n,"stability ball","swiss ball","exercise ball","physio ball"))return "Stability ball";
  if(containsAny(n,"bosu"))return "Bosu ball";
  if(containsAny(n,"battle rope","battling rope"))return "Battling Rope";
  if(containsAny(n,"rope","jump rope","skipping rope"))return "Rope";
  if(containsAny(n,"power sled"))return "Power Sled";
  if(containsAny(n,"sled"))return "Sled machine";
  if(containsAny(n,"suspension","trx"))return "Suspension";
  if(containsAny(n,"foam roll","foam roller","roller"))return "Roll";
  if(containsAny(n,"rollball","massage ball"))return "Rollball";
  if(containsAny(n,"leverage machine","leg press machine","machine","machine press","pec deck"))return "Leverage machine";
  if(containsAny(n,"stick","dowel"))return "Stick";
  if(containsAny(n,"vibration plate","vibrate plate"))return "Vibrate Plate";
  if(containsAny(n,"weighted","weight vest","weighted vest"))return "Weighted";
  if(containsAny(n,"hammer"))return "Hammer";
  if(containsAny(n,"pilates"))return "Pilates Machine";
  if(containsAny(n,"assisted"))return "Assisted";
  if(containsAny(n,"weightlift","snatch","clean and jerk"))return "Olympic barbell";
  if(containsAny(n,"bodyweight","body weight","push-up","push up","pushup","pull-up","pull up","pullup","squat","lunge","plank","stretch","jump","run","walk","sit-up","sit up","crunch"))return "Body weight";
  return "Body weight";
 }

 private static boolean containsAny(String value,String... terms){
  for(String term:terms)if(value.contains(term))return true;
  return false;
 }

 private final class ExerciseLibraryAdapter extends android.widget.BaseAdapter{
  private List<ExerciseLibraryRow> rows=Collections.emptyList();
  ExerciseLibraryAdapter(List<ExerciseLibraryRow> initial){setRows(initial);}
  void setRows(List<ExerciseLibraryRow> values){rows=values==null?Collections.emptyList():values;notifyDataSetChanged();}
  @Override public int getCount(){return rows.size();}
  @Override public ExerciseLibraryRow getItem(int position){return rows.get(position);}
  @Override public long getItemId(int position){return position;}
  @Override public View getView(int position,View convertView,android.view.ViewGroup parent){
   TextView text=convertView instanceof TextView?(TextView)convertView:new TextView(a);
   int padding=(int)(12*a.getResources().getDisplayMetrics().density);
   text.setPadding(padding,padding,padding,padding);
   text.setMinHeight((int)(44*a.getResources().getDisplayMetrics().density));
   text.setTextSize(16);
   ExerciseLibraryRow row=getItem(position);
   if(row!=null)text.setText(row.displayName("en".equals(selectedLanguage())));
   return text;
  }
 }

 private static final String[] EXERCISE_TYPES={"All","Strength","Aerobic","Stretching"};
 private static final String[] BODY_PARTS={"All","Back","Boxing","Calves","Cardio","Chest","Face","Feet","Forearms","Full body","Hands","Hips","Neck","Pilates","Plyometrics","Shoulders","Stretching","Thighs","Upper Arms","Waist","Weightlifting","Yoga"};
 private static final String[] EQUIPMENT_TYPES={"All","Assisted","Band","Barbell","Battling Rope","Body weight","Bosu ball","Cable","Dumbbell","EZ Barbell","Hammer","Kettlebell","Leverage machine","Medicine Ball","Olympic barbell","Pilates Machine","Power Sled","Resistance Band","Roll","Rollball","Rope","Sled machine","Smith machine","Stability ball","Stick","Suspension","Trap bar","Vibrate Plate","Weighted","Wheel roller"};

 private static final String[] GENDER_TYPES={"All","Male","Female"};

 private String categoryLabel(String key){
  if("All".equals(key))return tr("All","همه");
  if("Strength".equals(key))return tr("Strength","قدرتی");
  if("Aerobic".equals(key))return tr("Aerobic","هوازی");
  if("Stretching".equals(key))return tr("Stretching","کشش و انعطاف");
  if("Back".equals(key))return tr("Back","پشت");
  if("Boxing".equals(key))return tr("Boxing","بوکس");
  if("Calves".equals(key))return tr("Calves","ساق پا");
  if("Cardio".equals(key))return tr("Cardio","قلبی‌عروقی");
  if("Chest".equals(key))return tr("Chest","سینه");
  if("Face".equals(key))return tr("Face","صورت");
  if("Feet".equals(key))return tr("Feet","پا و کف پا");
  if("Forearms".equals(key))return tr("Forearms","ساعد");
  if("Full body".equals(key))return tr("Full body","کل بدن");
  if("Hands".equals(key))return tr("Hands","دست‌ها");
  if("Hips".equals(key))return tr("Hips","لگن و باسن");
  if("Neck".equals(key))return tr("Neck","گردن");
  if("Pilates".equals(key))return tr("Pilates","پیلاتس");
  if("Plyometrics".equals(key))return tr("Plyometrics","پلایومتریک");
  if("Shoulders".equals(key))return tr("Shoulders","سرشانه");
  if("Thighs".equals(key))return tr("Thighs","ران");
  if("Upper Arms".equals(key))return tr("Upper Arms","بازو");
  if("Waist".equals(key))return tr("Waist","شکم و کمر");
  if("Weightlifting".equals(key))return tr("Weightlifting","وزنه‌برداری");
  if("Weightlifts".equals(key))return tr("Weightlifts","حرکات وزنه‌برداری");
  if("Yoga".equals(key))return tr("Yoga","یوگا");
  if("Assisted".equals(key))return tr("Assisted","با کمک");
  if("Band".equals(key))return tr("Band","کش");
  if("Barbell".equals(key))return tr("Barbell","هالتر");
  if("Battling Rope".equals(key))return tr("Battling Rope","طناب بتل");
  if("Body weight".equals(key))return tr("Body weight","وزن بدن");
  if("Bosu ball".equals(key))return tr("Bosu ball","توپ بوسو");
  if("Cable".equals(key))return tr("Cable","سیم‌کش");
  if("Dumbbell".equals(key))return tr("Dumbbell","دمبل");
  if("EZ Barbell".equals(key))return tr("EZ Barbell","هالتر خمیده");
  if("Hammer".equals(key))return tr("Hammer","چکشی");
  if("Kettlebell".equals(key))return tr("Kettlebell","کتل‌بل");
  if("Leverage machine".equals(key))return tr("Leverage machine","دستگاه اهرمی");
  if("Medicine Ball".equals(key))return tr("Medicine Ball","توپ پزشکی");
  if("Olympic barbell".equals(key))return tr("Olympic barbell","هالتر المپیکی");
  if("Pilates Machine".equals(key))return tr("Pilates Machine","دستگاه پیلاتس");
  if("Power Sled".equals(key))return tr("Power Sled","سورتمه قدرتی");
  if("Resistance Band".equals(key))return tr("Resistance Band","کش مقاومتی");
  if("Roll".equals(key))return tr("Roll","فوم رول");
  if("Rollball".equals(key))return tr("Rollball","توپ ماساژ");
  if("Rope".equals(key))return tr("Rope","طناب");
  if("Sled machine".equals(key))return tr("Sled machine","دستگاه سورتمه");
  if("Smith machine".equals(key))return tr("Smith machine","دستگاه اسمیت");
  if("Stability ball".equals(key))return tr("Stability ball","توپ تعادلی");
  if("Stick".equals(key))return tr("Stick","چوب");
  if("Suspension".equals(key))return tr("Suspension","تی‌آر‌ایکس و تعلیقی");
  if("Trap bar".equals(key))return tr("Trap bar","هالتر شش‌ضلعی");
  if("Vibrate Plate".equals(key))return tr("Vibrate Plate","صفحه لرزشی");
  if("Weighted".equals(key))return tr("Weighted","با وزنه");
  if("Wheel roller".equals(key))return tr("Wheel roller","چرخ تمرین شکم");
  if("Male".equals(key))return tr("Male","آقایان");
  if("Female".equals(key))return tr("Female","بانوان");
  return key;
 }

 private Spinner makeCategorySpinner(String[] values,String hint){
  String[] labels=new String[values.length];
  for(int i=0;i<values.length;i++)labels[i]=categoryLabel(values[i]);
  Spinner spinner=new Spinner(a);
  spinner.setAdapter(new ArrayAdapter<String>(a,android.R.layout.simple_spinner_dropdown_item,labels){
   @Override public View getView(int position,View convertView,android.view.ViewGroup parent){
    TextView view=(TextView)super.getView(position,convertView,parent);
    view.setText(labels[position]);view.setSingleLine(true);view.setEllipsize(android.text.TextUtils.TruncateAt.END);
    return view;
   }
  });
  spinner.setPrompt(hint);
  return spinner;
 }

 private void showLoadedExerciseLibrary(ArrayList<ExerciseLibraryRow> exercises){
  if(exercises==null||exercises.isEmpty()){
   message(s("exercise_library"),tr("No GIF exercises were found in the exercise assets.","هیچ فایل GIF در پوشه حرکات پیدا نشد."));
   return;
  }

  final float density=a.getResources().getDisplayMetrics().density;
  EditText search=new EditText(a);
  search.setSingleLine(true);
  search.setHint(tr("Search in Persian or English","جست‌وجو به فارسی یا انگلیسی"));
  Spinner typeSpinner=makeCategorySpinner(EXERCISE_TYPES,tr("Exercise type","نوع حرکت"));
  Spinner bodySpinner=makeCategorySpinner(BODY_PARTS,tr("Body part","عضو بدن"));
  Spinner equipmentSpinner=makeCategorySpinner(EQUIPMENT_TYPES,tr("Equipment type","وسیله"));
  Spinner genderSpinner=makeCategorySpinner(GENDER_TYPES,tr("Gender","جنسیت"));

  LinearLayout content=column();
  content.setPadding((int)(10*density),(int)(8*density),(int)(10*density),(int)(8*density));
  content.addView(search,new LinearLayout.LayoutParams(-1,-2));

  LinearLayout firstFilterRow=new LinearLayout(a);
  firstFilterRow.setOrientation(LinearLayout.HORIZONTAL);
  LinearLayout secondFilterRow=new LinearLayout(a);
  secondFilterRow.setOrientation(LinearLayout.HORIZONTAL);
  addLibraryFilter(firstFilterRow,tr("Movement type","نوع حرکت"),typeSpinner);
  addLibraryFilter(firstFilterRow,tr("Body part","عضو بدن"),bodySpinner);
  addLibraryFilter(secondFilterRow,tr("Equipment","وسیله"),equipmentSpinner);
  addLibraryFilter(secondFilterRow,tr("Gender","جنسیت"),genderSpinner);
  content.addView(firstFilterRow,new LinearLayout.LayoutParams(-1,-2));
  content.addView(secondFilterRow,new LinearLayout.LayoutParams(-1,-2));

  ListView list=new ListView(a);
  list.setDividerHeight(Math.max(1,(int)density));
  ExerciseLibraryAdapter adapter=new ExerciseLibraryAdapter(exercises);
  list.setAdapter(adapter);
  content.addView(list,new LinearLayout.LayoutParams(-1,0,1f));

  LinearLayout previewPanel=new LinearLayout(a);
  previewPanel.setOrientation(LinearLayout.VERTICAL);
  previewPanel.setVisibility(View.GONE);
  previewPanel.setPadding(0,(int)(4*density),0,0);
  TextView selectedTitle=new TextView(a);
  selectedTitle.setTextSize(17);
  selectedTitle.setTypeface(null,android.graphics.Typeface.BOLD);
  TextView sourceLabel=new TextView(a);
  sourceLabel.setTextSize(11);
  ExerciseGifView selectedGif=new ExerciseGifView(a);
  previewPanel.addView(selectedTitle,new LinearLayout.LayoutParams(-1,-2));
  previewPanel.addView(sourceLabel,new LinearLayout.LayoutParams(-1,-2));
  previewPanel.addView(selectedGif,new LinearLayout.LayoutParams(-1,(int)(132*density)));
  content.addView(previewPanel,new LinearLayout.LayoutParams(-1,(int)(168*density)));

  final java.util.concurrent.atomic.AtomicInteger filterGeneration=new java.util.concurrent.atomic.AtomicInteger();
  final boolean[] dialogDismissed={false};
  Runnable filterRows=()->{
   String query=search.getText().toString().trim().toLowerCase(Locale.ROOT);
   String type=EXERCISE_TYPES[typeSpinner.getSelectedItemPosition()<0?0:typeSpinner.getSelectedItemPosition()];
   String body=BODY_PARTS[bodySpinner.getSelectedItemPosition()<0?0:bodySpinner.getSelectedItemPosition()];
   String equipment=EQUIPMENT_TYPES[equipmentSpinner.getSelectedItemPosition()<0?0:equipmentSpinner.getSelectedItemPosition()];
   String gender=GENDER_TYPES[genderSpinner.getSelectedItemPosition()<0?0:genderSpinner.getSelectedItemPosition()];
   int generation=filterGeneration.incrementAndGet();

   if(query.isEmpty()&&"All".equals(type)&&"All".equals(body)&&"All".equals(equipment)&&"All".equals(gender)){
    adapter.setRows(exercises);
    return;
   }

   // Filter potentially thousands of bundled GIF records off the UI thread.
   LIBRARY_EXECUTOR.execute(()->{
    ArrayList<ExerciseLibraryRow> filtered=new ArrayList<>();
    for(int i=0;i<exercises.size();i++){
     if(generation!=filterGeneration.get())return;
     ExerciseLibraryRow row=exercises.get(i);
     if(!"All".equals(type)&&!type.equals(row.exerciseType()))continue;
     if(!"All".equals(body)&&!body.equals(row.bodyPart()))continue;
     if(!"All".equals(equipment)&&!equipment.equals(row.equipment()))continue;
     if(!"All".equals(gender)&&!gender.equals(row.gender))continue;
     if(!query.isEmpty()&&!row.matchesSearch(query))continue;
     filtered.add(row);
    }
    new android.os.Handler(android.os.Looper.getMainLooper()).post(()->{
     if(!dialogDismissed[0]&&generation==filterGeneration.get())adapter.setRows(filtered);
    });
   });
  };

  android.widget.AdapterView.OnItemSelectedListener filterListener=new android.widget.AdapterView.OnItemSelectedListener(){
   public void onItemSelected(android.widget.AdapterView<?> parent,View view,int position,long id){filterRows.run();}
   public void onNothingSelected(android.widget.AdapterView<?> parent){}
  };
  typeSpinner.setOnItemSelectedListener(filterListener);
  bodySpinner.setOnItemSelectedListener(filterListener);
  equipmentSpinner.setOnItemSelectedListener(filterListener);
  genderSpinner.setOnItemSelectedListener(filterListener);

  android.os.Handler filterHandler=new android.os.Handler(android.os.Looper.getMainLooper());
  search.addTextChangedListener(new android.text.TextWatcher(){
   public void beforeTextChanged(CharSequence text,int start,int count,int after){}
   public void onTextChanged(CharSequence text,int start,int before,int count){
    filterHandler.removeCallbacks(filterRows);
    filterHandler.postDelayed(filterRows,140);
   }
   public void afterTextChanged(android.text.Editable editable){}
  });

  final AlertDialog libraryDialog=new AlertDialog.Builder(a)
    .setTitle(s("exercise_library"))
    .setView(content)
    .setNegativeButton(android.R.string.cancel,null)
    .create();

  list.setOnItemClickListener((parent,view,position,id)->{
   ExerciseLibraryRow selected=adapter.getItem(position);
   if(selected==null)return;
   selectedTitle.setText(selected.displayName("en".equals(selectedLanguage())));
   sourceLabel.setText(selected.sourceId==null||selected.sourceId.isEmpty()?"":tr("Source: GymVisual","منبع: GymVisual"));
   previewPanel.setVisibility(View.VISIBLE);
   selectedGif.setContentDescription(selected.displayName("en".equals(selectedLanguage())));
   selectedGif.setAssetName(selected.file);
  });
  libraryDialog.setOnDismissListener(dialog->{dialogDismissed[0]=true;filterGeneration.incrementAndGet();filterHandler.removeCallbacks(filterRows);});
  libraryDialog.setOnShowListener(dialog->{
   android.view.Window window=libraryDialog.getWindow();
   if(window!=null){
    android.util.DisplayMetrics metrics=a.getResources().getDisplayMetrics();
    window.setLayout((int)(metrics.widthPixels*0.96f),(int)(metrics.heightPixels*0.88f));
   }
   // The default/unfiltered state always starts with the complete exercise catalogue.
   search.setText("");
   typeSpinner.setSelection(0);
   bodySpinner.setSelection(0);
   equipmentSpinner.setSelection(0);
   genderSpinner.setSelection(0);
   list.setSelection(0);
   filterRows.run();
  });
  libraryDialog.show();
 }

 private void addLibraryFilter(LinearLayout row,String label,Spinner spinner){
  LinearLayout field=new LinearLayout(a);
  field.setOrientation(LinearLayout.VERTICAL);
  TextView caption=new TextView(a);
  caption.setText(label);
  caption.setTextSize(11);
  int pad=(int)(3*a.getResources().getDisplayMetrics().density);
  caption.setPadding(pad,pad,pad,0);
  field.addView(caption,new LinearLayout.LayoutParams(-1,-2));
  field.addView(spinner,new LinearLayout.LayoutParams(-1,-2));
  row.addView(field,new LinearLayout.LayoutParams(0,-2,1f));
 }

 private static final String[][] EXERCISE_NAME_TRANSLATIONS={
   {"ez-bar","هالتر خمیده"},{"smith machine","دستگاه اسمیت"},{"lat pulldown","لت از بالا"},{"pull down","لت از بالا"},{"pulldown","لت از بالا"},{"leg press","پرس پا"},{"chest press","پرس سینه"},{"shoulder press","پرس سرشانه"},{"leg extension","جلو پا دستگاه"},{"leg curl","پشت پا دستگاه"},{"hamstring curl","پشت پا"},{"hip thrust","هیپ تراست"},{"cable crossover","کراس اور کابل"},{"reverse fly","فلای معکوس"},{"face pull","فیس پول"},{"calf raise","ساق پا"},{"wrist curl","مچ‌جمع"},{"triceps pushdown","پشت بازو سیم‌کش"},{"bent over","خم‌شده"},{"bent-over","خم‌شده"},{"single leg","تک‌پا"},{"single arm","تک‌دست"},{"medicine ball","توپ پزشکی"},{"stability ball","توپ تعادلی"},{"version","نسخه"},{"male","آقایان"},{"female","بانوان"},{"with","با"},{"without","بدون"},
   {"dumbbell","دمبل"},{"barbell","هالتر"},{"kettlebell","کتل‌بل"},{"cable","کابل"},{"machine","دستگاه"},{"band","کش"},
   {"bodyweight","وزن بدن"},{"weight","وزنه"},{"plate","صفحه"},{"medicine ball","توپ پزشکی"},{"stability ball","توپ تعادلی"},
   {"push-up","شنا"},{"pushup","شنا"},{"pull-up","بارفیکس"},{"pullup","بارفیکس"},{"chin-up","بارفیکس دست برعکس"},
   {"squat","اسکوات"},{"lunge","لانج"},{"deadlift","ددلیفت"},{"press","پرس"},{"row","پارویی"},{"curl","جلو بازو"},{"extension","بازکردن"},
   {"flexion","خم‌کردن"},{"raise","بالا آوردن"},{"lift","بلندکردن"},{"fly","فلای"},{"flies","فلای"},{"crunch","کرانچ"},
   {"sit-up","درازونشست"},{"plank","پلانک"},{"bridge","پل"},{"kick","ضربه"},{"jump","پرش"},{"hop","جهش"},{"run","دویدن"},
   {"walk","پیاده‌روی"},{"stretch","کشش"},{"rotation","چرخش"},{"rotator","چرخاننده"},{"twist","چرخشی"},{"twisting","چرخش"},
   {"lateral","جانبی"},{"front","جلو"},{"rear","عقب"},{"back","پشت"},{"side","پهلو"},{"standing","ایستاده"},{"seated","نشسته"},
   {"sitting","نشسته"},{"lying","خوابیده"},{"kneeling","زانو‌زده"},{"half","نیمه"},{"single","تک‌پا"},{"one","یک"},{"two","دو"},
   {"alternate","متناوب"},{"alternating","متناوب"},{"alternated","متناوب"},{"arm","دست"},{"arms","دست‌ها"},{"leg","پا"},{"legs","پاها"},
   {"shoulder","شانه"},{"chest","سینه"},{"back","پشت"},{"upper","بالایی"},{"lower","پایینی"},{"abdominal","شکمی"},{"ab","شکم"},
   {"abs","شکم"},{"oblique","مورب"},{"hip","لگن"},{"hips","لگن"},{"glute","باسن"},{"glutes","باسن"},{"hamstring","همسترینگ"},
   {"quadriceps","چهارسر ران"},{"calf","ساق"},{"calves","ساق"},{"triceps","پشت بازو"},{"biceps","جلو بازو"},
   {"grip","گرفتن"},{"close","جمع"},{"wide","باز"},{"reverse","معکوس"},{"incline","بالاسینه"},{"decline","زیرسینه"},
   {"overhead","بالای سر"},{"over","روی"},{"under","زیر"},{"cross","ضربدری"},{"across","از عرض"},{"up","بالا"},{"down","پایین"},
   {"forward","جلو"},{"backward","عقب"},{"backwards","عقب"},{"forward","جلو"},{"floor","زمین"},{"wall","دیوار"},{"box","جعبه"},
   {"bench","نیمکت"},{"step","پله"},{"stair","پله"},{"rope","طناب"},{"rope","طناب"},{"bike","دوچرخه"},{"bicycle","دوچرخه"},
   {"cycling","دوچرخه‌سواری"},{"jumping","پرشی"},{"standing","ایستاده"},{"seated","نشسته"},{"prone","دمر"},{"supine","طاق‌باز"},
   {"isometric","ایزومتریک"},{"dynamic","پویا"},{"static","ثابت"},{"hold","نگه‌داشتن"},{"touch","لمس"},{"tap","ضربه"},
   {"toe","پنجه"},{"heel","پاشنه"},{"ankle","مچ پا"},{"wrist","مچ دست"},{"elbow","آرنج"},{"neck","گردن"},{"head","سر"},
   {"hand","دست"},{"hands","دست‌ها"},{"foot","پا"},{"feet","پاها"},{"finger","انگشت"},{"fingers","انگشتان"},
   {"deep","عمیق"},{"easy","آسان"},{"modified","اصلاح‌شده"},{"assisted","با کمک"},{"supported","با تکیه‌گاه"},
   {"explosive","انفجاری"},{"plyometric","پلایومتریک"},{"cardio","هوازی"},{"warm-up","گرم‌کردن"},{"cool-down","سردکردن"},
   {"yoga","یوگا"},{"pose","حالت"},{"stretching","کشش"},{"mobility","موبیلیتی"},{"balance","تعادل"},{"agility","چابکی"},
   {"drill","تمرین"},{"exercise","حرکت"},{"workout","تمرین"},{"the",""},{"and","و"},{"to","به"},{"from","از"},{"on","روی"},
   {"in","در"},{"at","در"},{"with","با"},{"of",""},{"for","برای"},{"a",""},{"an",""},{"male","آقایان"},{"version","نسخه"}
  };
 
 private static final java.util.regex.Pattern[] EXERCISE_NAME_PATTERNS=buildExerciseNamePatterns();
 private static java.util.regex.Pattern[] buildExerciseNamePatterns(){
  java.util.regex.Pattern[] result=new java.util.regex.Pattern[EXERCISE_NAME_TRANSLATIONS.length];
  for(int i=0;i<result.length;i++){
   result[i]=java.util.regex.Pattern.compile("(?<![a-z])"+java.util.regex.Pattern.quote(EXERCISE_NAME_TRANSLATIONS[i][0])+"(?![a-z])");
  }
  return result;
 }
 private static String persianExerciseName(String english){
  if(english==null||english.trim().isEmpty())return "";
  String value=english.toLowerCase(Locale.ROOT);

  for(int i=0;i<EXERCISE_NAME_TRANSLATIONS.length;i++)value=EXERCISE_NAME_PATTERNS[i].matcher(value).replaceAll(java.util.regex.Matcher.quoteReplacement(EXERCISE_NAME_TRANSLATIONS[i][1]));
  value=value.replaceAll("[()]", " ").replaceAll("[,/]", "،").replaceAll("\\s+"," ").trim();
  StringBuilder out=new StringBuilder();
  for(String token:value.split(" ")){
   if(token.isEmpty())continue;
   if(token.matches("[0-9.]+")){if(out.length()>0)out.append(' ');out.append(token);continue;}
   String translated=token;
   if(token.matches(".*[a-z].*"))translated=transliterateExerciseWord(token);
   if(out.length()>0)out.append(' ');
   out.append(translated);
  }
  return out.toString();
 }
 private static String transliterateExerciseWord(String word){
  String[][] sounds={{"tion","شن"},{"sion","ژن"},{"ough","اُف"},{"ight","ایت"},{"ee","ی"},{"ea","ی"},{"oo","و"},{"ou","او"},{"ow","او"},{"sh","ش"},{"ch","چ"},{"ph","ف"},{"th","ث"},{"ck","ک"},{"qu","کو"},{"x","کس"},{"a","َ"},{"b","ب"},{"c","ک"},{"d","د"},{"e","ِ"},{"f","ف"},{"g","گ"},{"h","ه"},{"i","ی"},{"j","ج"},{"k","ک"},{"l","ل"},{"m","م"},{"n","ن"},{"o","و"},{"p","پ"},{"q","ک"},{"r","ر"},{"s","س"},{"t","ت"},{"u","و"},{"v","و"},{"w","و"},{"y","ی"},{"z","ز"}};
  String result=word;
  for(String[] pair:sounds)result=result.replace(pair[0],pair[1]);
  return result;
 }

 private void showExerciseDetail(String name,String details,String gif,String sourceId){showExerciseDetail(name,details,gif,sourceId,false);}
 private void showExerciseDetail(String name,String details,String gif,String sourceId,boolean returnToLibrary){
  LinearLayout l=column();ExerciseGifView image=new ExerciseGifView(a,gif);image.setContentDescription(name);
  l.addView(image,new LinearLayout.LayoutParams(-1,(int)(220*a.getResources().getDisplayMetrics().density)));
  TextView title=new TextView(a);title.setText(name);title.setTextSize(20);l.addView(title);
  if(details!=null&&!details.trim().isEmpty()){TextView body=new TextView(a);body.setText(details);l.addView(body);}
  if(sourceId!=null&&!sourceId.isEmpty()){TextView attribution=new TextView(a);attribution.setText("© Gym visual — https://gymvisual.com/");l.addView(attribution);}
  AlertDialog detailDialog=new AlertDialog.Builder(a).setTitle(s("exercise_library")).setView(l).setPositiveButton(android.R.string.ok,null).create();
  if(returnToLibrary)detailDialog.setOnDismissListener(dialog->{if(!a.isFinishing()&&!a.isDestroyed())showLibrary();});
  detailDialog.show();
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
 private String tg(String en,String fa){return "en".equals(selectedLanguage())?en:fa;}

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
    savePendingBackupSections(selected);
    a.startActivityForResult(i,BACKUP);
   }).setNegativeButton(android.R.string.cancel,null).show();
 }
 void savePendingBackupSections(boolean[] selected){
  if(selected==null||selected.length!=4)throw new IllegalArgumentException("Exactly four backup sections are required");
  int mask=0;for(int index=0;index<selected.length;index++)if(selected[index])mask|=(1<<index);
  if(!a.getSharedPreferences("backup_restore_state",Context.MODE_PRIVATE).edit().putInt("pending_backup_sections",mask).commit())throw new IllegalStateException("Could not persist backup selection");
 }
 boolean[] loadPendingBackupSections(){
  int mask=a.getSharedPreferences("backup_restore_state",Context.MODE_PRIVATE).getInt("pending_backup_sections",0);
  boolean[] selected=new boolean[4];for(int index=0;index<selected.length;index++)selected[index]=(mask&(1<<index))!=0;
  return selected;
 }
 void clearPendingBackupSections(){
  a.getSharedPreferences("backup_restore_state",Context.MODE_PRIVATE).edit().remove("pending_backup_sections").apply();
 }
 private JSONObject createSelectiveBackup(boolean[] selected)throws Exception{
  boolean any=false;for(boolean value:selected)any|=value;
  if(!any)throw new IOException("No backup sections selected; selection may have been lost");
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
  Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("*/*");i.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"application/json","text/plain","application/octet-stream","application/x-sqlite3"});a.startActivityForResult(i,RESTORE);
 }
 public boolean onActivityResult(int requestCode,int resultCode,Intent data){
  if(requestCode!=BACKUP&&requestCode!=RESTORE)return false;
  if(resultCode!=Activity.RESULT_OK||data==null||data.getData()==null){
   if(requestCode==BACKUP)clearPendingBackupSections();
   return true;
  }
  try{
   if(requestCode==BACKUP){
    JSONObject backup=createSelectiveBackup(loadPendingBackupSections());
    byte[] payload=backup.toString(2).getBytes(java.nio.charset.StandardCharsets.UTF_8);
    if(payload.length>MAX_BACKUP_BYTES)throw new IOException("Backup exceeds the 25 MiB safety limit");
    try(OutputStream out=a.getContentResolver().openOutputStream(data.getData())){
     if(out==null)throw new IOException("No output stream");out.write(payload);out.flush();
    }
    clearPendingBackupSections();
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
    // Legacy SQLite backups replace the complete local database, so require an
    // explicit destructive-action confirmation before touching any user data.
    new AlertDialog.Builder(a)
     .setTitle(tr("Restore full database?","بازیابی کل پایگاه داده؟"))
     .setMessage(tr("This legacy backup replaces all workout and health records currently on this device. Export a backup first if you need to keep them.","این پشتیبان قدیمی تمام سوابق تمرین و سلامت فعلی گوشی را جایگزین می‌کند. اگر به اطلاعات فعلی نیاز دارید، ابتدا از آن‌ها پشتیبان بگیرید."))
     .setPositiveButton(tr("Replace all data","جایگزینی همهٔ اطلاعات"),(dialog,which)->{
      try{restoreLegacyDatabase(imported);}
      catch(Exception ex){message(tr("Error","خطا"),tr("Could not restore legacy database.","بازیابی پشتیبان قدیمی انجام نشد.")+"\n"+ex.getMessage());}
     })
     .setNegativeButton(android.R.string.cancel,null).show();
    return true;
   }
   JSONObject backup=new JSONObject(new String(imported,java.nio.charset.StandardCharsets.UTF_8));
   if(!"fityar-selective-backup".equals(backup.optString("format"))||backup.optInt("version")!=1||!(backup.opt("sections") instanceof JSONArray))throw new IOException("Unsupported backup format");
   JSONArray sections=backup.getJSONArray("sections");
   ArrayList<JSONObject> valid=new ArrayList<>();
   ArrayList<String> labels=new ArrayList<>();
   ArrayList<String> ids=new ArrayList<>();
   HashSet<String> seenSectionIds=new HashSet<>();
   for(int i=0;i<sections.length();i++){
    JSONObject section=sections.optJSONObject(i);
    if(section==null)throw new IOException("Backup section must be an object");
    String id=section.optString("id","");
    if(!Arrays.asList("workouts","health","steps","preferences").contains(id))continue;
    if(!seenSectionIds.add(id))throw new IOException("Duplicate backup section: "+id);
    boolean shapeValid;
    String label;
    switch(id){
     case "workouts":
      shapeValid=section.opt("plans") instanceof JSONArray&&section.opt("exercises") instanceof JSONArray&&section.opt("logs") instanceof JSONArray;
      label=tr("Workout plans & logs","برنامه‌ها و سوابق تمرین");break;
     case "health":
      shapeValid=section.opt("sleep_logs") instanceof JSONArray&&section.opt("vital_logs") instanceof JSONArray&&section.opt("body_metrics") instanceof JSONArray;
      label=tr("Health records","سوابق سلامت");break;
     case "steps":
      shapeValid=section.opt("step_counter") instanceof JSONObject;
      label=tr("Step counter history","سوابق قدم‌شمار");break;
     default:
      shapeValid=section.opt("fityar_preferences") instanceof JSONObject&&section.opt("fit_settings") instanceof JSONObject;
      label=tr("App preferences","تنظیمات برنامه");break;
    }
    if(!shapeValid)throw new IOException("Malformed backup section: "+id);
    valid.add(section);ids.add(id);labels.add(label);
   }
   if(valid.isEmpty())throw new IOException("No supported backup sections");
   boolean[] chosen=new boolean[valid.size()];Arrays.fill(chosen,true);
   new AlertDialog.Builder(a).setTitle(tr("Choose sections to restore","انتخاب بخش‌های بازیابی")).setMultiChoiceItems(labels.toArray(new String[0]),chosen,(dialog,which,checked)->chosen[which]=checked)
    .setPositiveButton(tr("Restore selected","بازیابی بخش‌های انتخاب‌شده"),(dialog,which)->{
     boolean any=false;for(boolean value:chosen)any|=value;
     if(!any){message(tr("Error","خطا"),tr("Select at least one section.","حداقل یک بخش را انتخاب کنید."));return;}
     new AlertDialog.Builder(a).setTitle(tr("Confirm restore","تأیید بازیابی")).setMessage(tr("Selected sections will replace their current data. Other sections will remain unchanged.","بخش‌های انتخاب‌شده جایگزین اطلاعات فعلی همان بخش می‌شوند؛ سایر بخش‌ها تغییر نمی‌کنند."))
      .setPositiveButton(tr("Restore","بازیابی"),(confirm,button)->{
       final boolean[] preferencesRestored={false};
       try{db.getWritableDatabase().beginTransaction();
        try{
         for(int i=0;i<valid.size();i++)if(chosen[i]&&!"preferences".equals(ids.get(i))&&!"steps".equals(ids.get(i)))restoreSection(valid.get(i),ids.get(i));
         db.getWritableDatabase().setTransactionSuccessful();
        }finally{db.getWritableDatabase().endTransaction();}
        for(int i=0;i<valid.size();i++)if(chosen[i]&&"steps".equals(ids.get(i))){
         a.stopService(new Intent(a,StepCounterService.class));
         restorePreferences("step_counter",valid.get(i).getJSONObject("step_counter"));
         if(a instanceof MainActivity)((MainActivity)a).refreshStepCounterAfterRestore();
        }
        for(int i=0;i<valid.size();i++)if(chosen[i]&&"preferences".equals(ids.get(i))){
         preferencesRestored[0]=true;
         JSONObject section=valid.get(i);
         restorePreferences("fityar_preferences",section.getJSONObject("fityar_preferences"));
         restorePreferences("fit_settings",section.getJSONObject("fit_settings"));
         Context app=a.getApplicationContext();android.content.SharedPreferences prefs=app.getSharedPreferences("fityar_preferences",Context.MODE_PRIVATE);
         if(prefs.getBoolean("reminder_enabled",false))WorkoutReminderReceiver.schedule(app,prefs.getInt("reminder_hour",19),prefs.getInt("reminder_minute",0));
         else WorkoutReminderReceiver.cancel(app);
        }
        if(preferencesRestored[0]){
         new AlertDialog.Builder(a).setTitle(tr("Restore complete","بازیابی کامل شد"))
          .setMessage(tr("Selected sections were restored. Other sections were left unchanged. Restart the screen to apply restored language, theme, and display settings.","بخش‌های انتخاب‌شده بازیابی شدند و سایر بخش‌ها بدون تغییر ماندند. برای اعمال زبان، پوسته و تنظیمات نمایش بازیابی‌شده، صفحه دوباره بارگذاری می‌شود."))
          .setPositiveButton(android.R.string.ok,(done,buttonIndex)->a.recreate()).show();
        }else message(tr("Restore complete","بازیابی کامل شد"),tr("Selected sections were restored. Other sections were left unchanged.","بخش‌های انتخاب‌شده بازیابی شدند و سایر بخش‌ها بدون تغییر ماندند."));
       }catch(Exception ex){message(tr("Error","خطا"),tr("Could not restore backup. Current data was kept where possible.","بازیابی انجام نشد؛ تا حد امکان اطلاعات فعلی حفظ شدند.")+"\n"+ex.getMessage());}
      }).setNegativeButton(android.R.string.cancel,null).show();
    }).setNegativeButton(android.R.string.cancel,null).show();
  }catch(Exception ex){
   if(requestCode==BACKUP)clearPendingBackupSections();
   message(tr("Error","خطا"),tr("Invalid or unreadable backup file.","فایل پشتیبان نامعتبر است یا خوانده نمی‌شود.")+"\n"+ex.getMessage());
  }
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
   int versionNumber;
   try(Cursor version=check.rawQuery("PRAGMA user_version",null)){
    if(!version.moveToFirst())throw new IOException("Missing SQLite version");
    versionNumber=version.getInt(0);
    if(versionNumber<0||versionNumber>4)throw new IOException("Backup comes from an unsupported app version");
   }
   java.util.Map<String,java.util.Set<String>> required=new java.util.LinkedHashMap<>();
   required.put("plans",new java.util.HashSet<>(java.util.Arrays.asList("id","name")));
   required.put("exercises",new java.util.HashSet<>(java.util.Arrays.asList("id","plan_id","name","sets","reps")));
   required.put("logs",new java.util.HashSet<>(java.util.Arrays.asList("id","date_jalali","plan_name","exercise_name","sets","reps","weight","created_at")));
   if(versionNumber>=4){
    required.get("logs").addAll(java.util.Arrays.asList("rest_seconds","effort","notes"));
    required.put("sleep_logs",new java.util.HashSet<>(java.util.Arrays.asList("id","start_at","end_at","duration_minutes")));
    required.put("vital_logs",new java.util.HashSet<>(java.util.Arrays.asList("id","measured_at","systolic","diastolic","pulse")));
    required.put("body_metrics",new java.util.HashSet<>(java.util.Arrays.asList("id","measured_at","weight_kg","waist_cm")));
   }
   for(java.util.Map.Entry<String,java.util.Set<String>> entry:required.entrySet()){
    java.util.HashSet<String> actual=new java.util.HashSet<>();
    try(Cursor columns=check.rawQuery("PRAGMA table_info("+entry.getKey()+")",null)){
     while(columns.moveToNext())actual.add(columns.getString(1));
    }
    if(!actual.containsAll(entry.getValue()))throw new IOException("Incompatible FitYar table schema: "+entry.getKey());
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
  }else if("steps".equals(id)){
   // Never restore a live service state; an imported history must remain paused.
   restorePreferences("step_counter",section.getJSONObject("step_counter"));
   a.getSharedPreferences("step_counter",Context.MODE_PRIVATE).edit()
    .putBoolean("running",false).putBoolean("paused",false).apply();
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
  ByteArrayOutputStream bytes=new ByteArrayOutputStream();byte[] buffer=new byte[8192];int count,total=0;
  while((count=in.read(buffer))!=-1){total+=count;if(total>MAX_BACKUP_BYTES)throw new IOException("Backup exceeds the 25 MiB safety limit");bytes.write(buffer,0,count);}
  return bytes.toByteArray();
 }
 private void copy(InputStream in,OutputStream out)throws IOException{
  byte[] buffer=new byte[8192];int count;while((count=in.read(buffer))!=-1)out.write(buffer,0,count);out.flush();
 }
}
