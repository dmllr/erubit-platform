package u;

import android.content.Context;
import android.os.Build;
import android.text.Html;
import android.text.Spanned;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.Random;

import a.erubit.platform.android.App;

public class U {
    public static String loadStringResource(Context context, int id) throws IOException {
        Writer writer = new StringWriter();
        char[] buffer = new char[1024];
        try (InputStream is = context.getResources().openRawResource(id)) {
            Reader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            int n;
            while ((n = reader.read(buffer)) != -1) {
                writer.write(buffer, 0, n);
            }
        }

        return writer.toString();
    }

    public static void shuffleIntArray(int[] array)
    {
        int index;
        Random random = new Random();
        for (int i = array.length - 1; i > 0; i--)
        {
            index = random.nextInt(i + 1);
            if (index != i)
            {
                array[index] ^= array[i];
                array[i] ^= array[index];
                array[index] ^= array[i];
            }
        }
    }

    public static void shuffleStrArray(String[] arr) {
        Random rgen = new Random();

        for (int i = 0; i < arr.length; i++) {
            int randPos = rgen.nextInt(arr.length);
            String tmp = arr[i];
            arr[i] = arr[randPos];
            arr[randPos] = tmp;
        }
    }

    public static void animateNegative(View view) {
        Animation animation = new TranslateAnimation(-16, +16, 0, 0);
        animation.setDuration(100);
        animation.setFillAfter(false);
        animation.setRepeatCount(3);
        animation.setRepeatMode(Animation.REVERSE);

        view.startAnimation(animation);
    }

    @SuppressWarnings("deprecation")
    public static Spanned getSpanned(String text) {
        Spanned spanned;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            spanned = Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY);
        else
            spanned = Html.fromHtml(text);
        return spanned;
    }

    public static String defurigana(String text) {
        // Don't trust Lint, regex is correct
        return text.replaceAll("\\{(.*?):(.*?)\\}", "$1");
    }

    public static boolean equals(String s1, String s2) {
        if (s1 == null || s2 == null)
            return false;

        String d1 = defurigana(s1).replace('　', ' ');
        String d2 = defurigana(s2).replace('　', ' ');

        return d1.equals(d2);
    }

    public static boolean equalsIndependent(String s1, String s2) {
        String regex = "\\s+:\\s+";
        String[] w1 = defurigana(s1).split(regex);
        String[] w2 = defurigana(s2).split(regex);
        Arrays.sort(w1);
        Arrays.sort(w2);

        return Arrays.equals(w1, w2);
    }

    public static String getStringValue(Context context, JSONObject jo, String property) throws JSONException {
        String value = "";
        if (jo.has(property)) {
            Object djo = jo.get(property);
            if (djo instanceof JSONArray) {
                JSONArray jd = jo.getJSONArray(property);
                for (int k = 0; k < jd.length(); k++)
                    value += jd.getString(k);
            } else {
                value = (String)djo;
                if (value.startsWith(C.RESREF)) {
                    String resourceName = value.substring(C.RESREF.length());
                    int resourceId = context.getResources().getIdentifier(resourceName, "raw", context.getPackageName());
                    try {
                        value = U.loadStringResource(context, resourceId);
                    } catch (IOException e) {
                        value = "";
                    }
                }
            }
        }
        return value;
    }
}
