package com.morrisware.nestedscrolllayout;

import org.junit.Assert;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() {
        assertEquals(4, 2 + 2);
    }

    private static final String PATH = "C:\\Users\\mmw\\Desktop\\com.kaola\\res\\layout";

    private List<File> findViewPager() throws IOException {
        List<File> data = new ArrayList<>();

        File source = new File(PATH);
        if (source.isDirectory()) {
            for (File file : source.listFiles()) {
                if (file.isFile()) {
                    FileInputStream fis = new FileInputStream(file);
                    BufferedReader br = new BufferedReader(new InputStreamReader(fis));
                    boolean includeViewPager = false;
                    boolean includeTabLayout = false;
                    boolean includeBannerView = false;

                    String line;
                    while ((line = br.readLine()) != null) {
                        if (line.contains("ViewPager")) {
                            includeViewPager = true;
                        }
                        if (line.contains("SmartTabLayout")) {
                            includeTabLayout = true;
                        }
                        if (line.contains("BannerView")) {
                            includeBannerView = true;
                        }
//                        if (includeTabLayout && includeViewPager) {
//                            data.add(file.getName());
//                            break;
//                        }

                        if (includeTabLayout) {
                            data.add(file);
                            break;
                        }
                    }
                }
            }
        }
        return data;
    }

    @Test
    public void testFindViewPager() throws Exception {
        List<File> data = findViewPager();
        System.out.println(data.size());

        PrintWriter pw = new PrintWriter(new OutputStreamWriter(System.out));
        for (File file : data) {
            FileInputStream fis = new FileInputStream(file);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(fis));

            pw.println(file.getName());

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                pw.println(line);
            }

            fis.close();
        }
        pw.close();

//        System.out.println(data.toString());
    }

}