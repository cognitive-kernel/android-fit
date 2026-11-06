package com.cognitivekernel.androidfit;

import java.util.Locale;

/** Shared Persian display-name localization for exercise catalog and workout screens. */
final class ExerciseNameLocalizer {
    private ExerciseNameLocalizer() {}

    static String toPersian(String english){
      if(english==null||english.trim().isEmpty())return "";
      String value=english.toLowerCase(Locale.ROOT);
      String[][] words={
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
      for(String[] pair:words)value=value.replaceAll("(?<![a-z])"+java.util.regex.Pattern.quote(pair[0])+"(?![a-z])",java.util.regex.Matcher.quoteReplacement(pair[1]));
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
}
