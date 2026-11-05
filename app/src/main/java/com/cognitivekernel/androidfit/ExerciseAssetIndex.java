package com.cognitivekernel.androidfit;

import android.content.Context;
import android.content.res.AssetManager;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Immutable in-memory index of bundled exercise GIFs. The first lookup is cold-start safe. */
public final class ExerciseAssetIndex {
    private static final Pattern ID_SUFFIX = Pattern.compile("(?:_\\[?(\\d{3,10})\\]?|\\s+\\[?(\\d{3,10})\\]?)$");
    private static volatile List<ExerciseAsset> cached;

    public static final class ExerciseAsset {
        public final String id,gender,name,file,persianName;
        private ExerciseAsset(String id,String gender,String name,String file) {
            this.id=id;this.gender=gender;this.name=name;this.file=file;
            this.persianName=AdvancedFeatures.persianExerciseName(name);
        }
    }

    private ExerciseAssetIndex() {}

    public static ExerciseAsset findBest(Context context,String exerciseName,String gender) {
        if(exerciseName==null||exerciseName.trim().isEmpty())return null;
        String query=normalize(exerciseName);
        if(query.isEmpty())return null;
        ExerciseAsset best=null;
        int bestScore=0,bestGenderRank=-1;
        for(ExerciseAsset candidate:assets(context)) {
            int rank=genderRank(candidate.gender,gender);
            if(rank<0)continue;
            int score=Math.max(score(query,normalize(candidate.name)),
                score(query,normalize(candidate.persianName)));
            if(score>bestScore||(score==bestScore&&rank>bestGenderRank)
                    ||(score==bestScore&&rank==bestGenderRank&&best!=null&&candidate.name.length()<best.name.length())) {
                best=candidate;bestScore=score;bestGenderRank=rank;
            }
        }
        return bestScore>=Math.max(6,query.replace(" ","").length()/3)?best:null;
    }

    public static String findFile(Context context,String exerciseName,String gender) {
        ExerciseAsset result=findBest(context,exerciseName,gender);
        return result==null?null:"exercises/"+result.file;
    }

    public static String findFileById(Context context,String id,String gender) {
        if(id==null||!id.matches("\\d{3,10}"))return null;
        ExerciseAsset best=null;
        int bestRank=-1;
        for(ExerciseAsset candidate:assets(context)) {
            if(!id.equals(candidate.id))continue;
            int rank=genderRank(candidate.gender,gender);
            if(rank>bestRank){best=candidate;bestRank=rank;}
        }
        return best==null?null:"exercises/"+best.file;
    }

    private static int genderRank(String candidate,String requested) {
        if(requested==null||requested.trim().isEmpty())return 1;
        String wanted=requested.toLowerCase(Locale.ROOT);
        if(wanted.equals("all"))return candidate.equals("all")?2:-1;
        if(!wanted.equals("male")&&!wanted.equals("female"))return -1;
        if(candidate.equals(wanted))return 2;
        if(candidate.equals("all"))return 1;
        return -1;
    }

    private static List<ExerciseAsset> assets(Context context) {
        List<ExerciseAsset> result=cached;
        if(result!=null)return result;
        synchronized(ExerciseAssetIndex.class) {
            if(cached!=null)return cached;
            ArrayList<ExerciseAsset> loaded=new ArrayList<>(6556);
            try { collect(context.getApplicationContext().getAssets(),"exercises",loaded); }
            catch(IOException error) { throw new IllegalStateException("Could not index bundled exercise GIFs",error); }
            Collections.sort(loaded,(left,right)->left.file.compareToIgnoreCase(right.file));
            cached=Collections.unmodifiableList(loaded);
            return cached;
        }
    }

    private static void collect(AssetManager manager,String directory,List<ExerciseAsset> output)throws IOException {
        String[] entries=manager.list(directory);
        if(entries==null)return;
        for(String entry:entries) {
            String path=directory+"/"+entry;
            if(entry.toLowerCase(Locale.ROOT).endsWith(".gif")) {
                ExerciseAsset parsed=parse(entry);
                if(parsed!=null)output.add(parsed);
            } else if(!entry.contains(".")) collect(manager,path,output);
        }
    }

    private static ExerciseAsset parse(String filename) {
        String base=filename.substring(0,filename.length()-4);
        Matcher idMatcher=ID_SUFFIX.matcher(base);
        if(!idMatcher.find())return null;
        String id=idMatcher.group(1)!=null?idMatcher.group(1):idMatcher.group(2);
        String withoutId=base.substring(0,idMatcher.start()).replaceAll("[_\\s]+$","");
        String lower=withoutId.toLowerCase(Locale.ROOT);
        String gender="all";
        if(lower.contains("(male)")||lower.endsWith(")male")||lower.endsWith(" male"))gender="male";
        else if(lower.contains("(female)")||lower.endsWith(")female")||lower.endsWith(" female"))gender="female";
        String name=withoutId.replaceAll("(?i)\\s*\\((male|female)\\)"," ")
            .replaceAll("(?i)\\s+(male|female)$"," ")
            .replaceAll("(?i)(?<=\\))(male|female)$"," ")
            .replace('_',' ').replaceAll("\\s+"," ").trim();
        if(name.isEmpty())return null;
        return new ExerciseAsset(id,gender,name,filename);
    }

    private static int score(String query,String candidate) {
        if(query.equals(candidate))return 10000;
        if(candidate.isEmpty())return 0;
        if(candidate.startsWith(query))return 5000+query.length();
        if(candidate.contains(query))return 3000+query.length();
        // Gender-constrained resolution must not choose a merely similar exercise.
        return 0;
    }

    private static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT).replaceAll("(?i)\\s*\\((male|female)\\)"," ").replaceAll("(?i)\\s+(male|female)$"," ").replaceAll("[_\\-]+"," ")
            .replaceAll("[^a-z0-9\\u0600-\\u06ff ]"," ")
            .replaceAll("\\s+"," ").trim();
    }
}
