import binning.Aggregator;
import binning.BinManager;
import binning.VariableContext;
import binning.operator.BinningConfig;
import binning.operator.BinningOp;
import binning.operator.FormatterConfig;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.TiePointGeoCoding;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.beam.util.StringUtils;
import org.esa.beam.util.converters.JtsGeometryConverter;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.lang.String;

public class BinningCalc {
    public static void calc(List<String> filePaths, String[] varNames, Float[] bounds, String validExpression) {

        try {
            BinningOp binningOp = new BinningOp();

            MyVariableContext variableContext = new MyVariableContext(varNames);

            int aggregatorCount = 5;
            Aggregator[] aggregators = new Aggregator[varNames.length * aggregatorCount];
            for (int i = 0; i < varNames.length; i++) {
                aggregators[aggregatorCount * i] = new AggregatorAverage2(variableContext, varNames[i], 1.0, true, false);
                aggregators[aggregatorCount * i + 1] = new AggregatorMedian(variableContext, varNames[i]);
                aggregators[aggregatorCount * i + 2] = new AggregatorMinMax(variableContext, varNames[i]);
                aggregators[aggregatorCount * i + 3] = new AggregatorPercentile(variableContext, varNames[i], 25);
                aggregators[aggregatorCount * i + 4] = new AggregatorPercentile(variableContext, varNames[i], 75);
            }

            variableContext.setValidMaskExpression(validExpression);


            /*BinManager binManager = new BinManager(variableContext,
                    new AggregatorAverage2(variableContext, "MCI", 1.0, true, false),
                    new AggregatorMedian(variableContext, "MCI"),
                    new AggregatorMinMax(variableContext, "MCI"),
                    new AggregatorPercentile(variableContext, "MCI", 25),
                    new AggregatorPercentile(variableContext, "MCI", 75),
                    new AggregatorAverage2(variableContext, "CY", 1.0, true, false)); */

            BinManager binManager = new BinManager(variableContext, aggregators);

            //System.out.println(StringUtils.join(binManager.getOutputFeatureNames(), ","));

            /*AggregatorAverage.Config MCIAvg = new AggregatorAverage.Config();
            MCIAvg.setVarName("MCI");
            AggregatorAverage.Config CYAvg = new AggregatorAverage.Config();
            CYAvg.setVarName("CY");
            AggregatorMedian.Config MCIMedian = new AggregatorMedian.Config();
            MCIAvg.setVarName("MCI");
            AggregatorMinMax.Config MCIMinMax = new AggregatorMinMax.Config();
            MCIMinMax.setVarName("MCI");
            AggregatorPercentile.Config MCIp25 = new AggregatorPercentile.Config();
            MCIp25.setVarName("MCI");
            MCIp25.setPercentage(25);
            AggregatorPercentile.Config MCIp75 = new AggregatorPercentile.Config();
            MCIp75.setVarName("MCI");
            MCIp75.setPercentage(75);*/
            final BinningConfig binningConfig = new BinningConfig();
            binningConfig.setBinManager(binManager);

            binningConfig.setSuperSampling(2);

            binningOp.setParameter("outputBinnedData", false);

            //binningConfig.setAggregatorConfigs(MCIAvg, MCIMedian, MCIp25, MCIp75, MCIMinMax, CYAvg);

            binningConfig.setNumRows(67000);
            binningConfig.setMaskExpr(validExpression);
            binningOp.setBinningConfig(binningConfig);


            FormatterConfig formatterConfig = createFormatterConfig();
            binningOp.setFormatterConfig(formatterConfig);

            //System.out.println(filePaths.toString());


            try {
                for (String filePath: filePaths) {
                    //System.out.println(filePath);
                    Product product = ProductIO.readProduct(filePath);
                    String productType = product.getProductType();
                    //System.out.println(productType);

                    //int rasterWidth = product.getSceneRasterWidth();
                    //int rasterHeight = product.getSceneRasterHeight();

                    //Product targetProduct = new Product(new File(filePath + "_test.dim").getName(), "MER_FR__3P", rasterWidth, rasterHeight);

                    //TiePointGrid latitude = product.getTiePointGrid("latitude");
                    //TiePointGrid longitude = product.getTiePointGrid("longitude");
                    //System.out.println(latitude.getPixelDouble(2240, 2240));
                    //System.out.println(longitude.getPixelDouble(0, 0));
                    //System.out.println(latitude.getSceneRasterWidth());
                    //System.out.println(latitude.getSceneRasterHeight());

                    //if (productType.equals("MER_FR__2P")) {
                        //Band reflec3Band = product.getBand("reflec_3");
                        //Raster reflec3 = reflec3Band.getSourceImage().getData();
                        //System.out.println(reflec3.getWidth());
                        //System.out.println(reflec3.getHeight());
                        //System.out.println("add product");
                        binningOp.setSourceProduct(filePath, product);

                    //}


                    //product.dispose();
                }
            } catch (IOException e) {
                System.out.println("I/O error: " + e.getMessage());
                e.printStackTrace();
            }

            /*float obs1 = 0.2F;
            float obs2 = 0.4F;
            float obs3 = 0.6F;
            float obs4 = 0.8F;
            float obs5 = 1.0F;

            binningOp.setSourceProducts(createSourceProduct(obs1),
                    createSourceProduct(obs2),
                    createSourceProduct(obs3),
                    createSourceProduct(obs4));*/

            //binningOp.setParameter("sourceProductPaths", filePaths.toArray(new String[filePaths.size()]));
            //binningOp.setParameter("sourceProductPaths", StringUtils.join(filePaths.toArray(new String[filePaths.size()]), ","));

            //System.out.println(StringUtils.join(filePaths.toArray(new String[filePaths.size()]), ","));
            //System.out.println(filePaths.toArray(new String[filePaths.size()]) == null);
            //System.out.println("sourceProductPaths: " + binningOp.getParameter("sourceProductPaths"));

            //System.out.println("length: " + binningOp.getSourceProducts().length);

            GeometryFactory gf = new GeometryFactory();
            binningOp.setRegion(gf.createPolygon(gf.createLinearRing(new Coordinate[]{
                    new Coordinate(bounds[0], bounds[3]),
                    new Coordinate(bounds[1], bounds[3]),
                    new Coordinate(bounds[1], bounds[2]),
                    new Coordinate(bounds[0], bounds[2]),
                    new Coordinate(bounds[0], bounds[3]),
            }), null));

            //JtsGeometryConverter geometryConverter = new JtsGeometryConverter();
            //binningOp.setRegion(geometryConverter.parse("POLYGON ((15 55, 15 66, 30 66, 30 55, 15 55))"));

            //System.out.println("length: " + binningOp.getSourceProducts().length);
            //binningOp.initialize();
            final Product targetProduct = binningOp.getTargetProduct();

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
        }

    }

    static FormatterConfig createFormatterConfig() throws IOException {
        final File targetFile = new File("l3_out.dim");
        final FormatterConfig formatterConfig = new FormatterConfig();
        formatterConfig.setOutputFile(targetFile.getPath());
        formatterConfig.setOutputType("Product");
        formatterConfig.setOutputFormat("BEAM-DIMAP");
        return formatterConfig;
    }
}
