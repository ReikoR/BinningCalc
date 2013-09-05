import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class Binning {

    public static void main(String[] args) {

        long startTime = System.currentTimeMillis();
        if (args.length == 1) {
            String[] varNames;
            Float[] bounds = new Float[4];

            Properties settings = new Properties();

            try {
                settings.load(new FileInputStream(args[0]));
            } catch (IOException e) {
                e.printStackTrace();
            }

            varNames = settings.getProperty("bands").replaceAll("\\s","").split(",");
            String[] region_bounds = settings.getProperty("region_bounds").replaceAll("\\s","").split(",");
            String valid_expression = settings.getProperty("valid_expression");

            for (int i = 0; i < bounds.length && i < region_bounds.length; i++) {
                bounds[i] = Float.parseFloat(region_bounds[i]);
            }

            System.out.println(StringUtils.join(varNames, ", "));
            System.out.println(bounds[0] + " " + bounds[1] + " " + bounds[2] + " " + bounds[3]);
            System.out.println(valid_expression);

            File[] dirs = new File(".").listFiles();

            List<String> fileNames = new ArrayList<String>();
            for (File dir : dirs) {
                //if (dir.isFile() && dir.getName().matches("MER_FR__2P.*\\.N1")) {
                //System.out.println(dir.getName());
                //if (dir.isFile() && dir.getName().matches("([^\\s]+(\\.(?i)(N1\\.dim))$)")) {
                if (dir.isFile() && dir.getName().matches(".*\\.dim")) {
                    //System.out.println(dir.getName());
                    fileNames.add(dir.getName());
                    //BeamTest.calcIndex(dir.getAbsolutePath());
                    //t.start();
                    //System.out.println(t.getState());
                    //Thread.State.NEW
                }
            }
            System.out.println("Files: " + fileNames.size());
            BinningCalc.calc(fileNames, varNames, bounds, valid_expression);
        } else {
            System.out.println("No settings file specified");
        }
        System.out.println("\nAll done in " + (System.currentTimeMillis() - startTime) + "ms");
    }
}
