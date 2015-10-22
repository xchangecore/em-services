package com.leidos.xchangecore.core.em.util;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.Transparency;
import java.awt.geom.AffineTransform;
import java.awt.geom.Dimension2D;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.usersmarts.geo.render.ProjectionUtility;
import com.usersmarts.util.Coerce;
import com.usersmarts.util.DOMUtils;
import com.vividsolutions.jts.geom.Envelope;

/**
 * MapRenderer
 * 
 * renders WMS Web Map Context documents, also known as View Context documents, by requesting
 * individual layers as images from their respective services and compositing those images into one
 * final image
 * 
 * @author Patrick Neal - Image Matters, LLC
 * @package com.saic.dctd.uicds.core.service
 * @created Nov 24, 2008
 * @ssdd
 */
public class MapRenderer {

    private final static Color BACKGROUND_COLOR = new Color(0.1F, 0.2F, 0.3F, 0.0F);

    Logger log = LoggerFactory.getLogger(getClass());

    /**
     * This method renders the <code>layers</code> from the <code>viewContext</code> into an image
     * of the specified <code>width</code> and <code>height</code> contrained by the
     * <code>bbox</code>.
     * 
     * @param viewContext Node root node in ViewContext document
     * @param bbox Envelope spatial extent of map
     * @param width int x-dimension of map image
     * @param height int y-dimension of map image
     * @param format String mimetype of resulting map image and propogated to the map services
     *            providing each component layer.
     * @return Image composited map image
     * @throws Exception
     * @ssdd
     */
    public Image render(Element viewContext,
                        String[] layers,
                        Envelope bbox,
                        int width,
                        int height,
                        String format) throws Exception {

        String formatType = format.substring(format.indexOf("/") + 1, format.length());
        BufferedImage image = createImage(formatType, width, height, true);

        Dimension2D window = new Dimension(width, height);
        Graphics2D graphics = (Graphics2D) image.getGraphics();
        AffineTransform t = ProjectionUtility.total(window, bbox);
        graphics.setTransform(t);

        graphics.setColor(BACKGROUND_COLOR);

        Element layerList = DOMUtils.getChild(viewContext, "LayerList");
        List<String> requests = getLayerRequests(layerList, layers, bbox, width, height, format);
        for (String url : requests) {
            fetchImage(url, graphics); // retrieve map image
            image.flush();
        }

        return image;
    }

    /**
     * This method returns a list of urls for retrieving the individual layers of the map. It will
     * coalesce adjacent layers into a single request, when possible.
     * 
     * @param layerList Element parent node to the list of layer Elements within the ViewContext
     * @param layers String[] of layer names
     * @param bbox Envelope spatial extent of the map image
     * @param width x-dimension of the map image
     * @param height y-dimension of the map image
     * @param format String mime-type of the map image
     * @return List of urls for get map requests
     * @ssdd
     */
    private List<String> getLayerRequests(Element layerList,
                                          String[] layers,
                                          Envelope bbox,
                                          int width,
                                          int height,
                                          String format) {

        List<String> requests = new ArrayList<String>();
        String lastServer = null;

        for (Element elem : DOMUtils.getChildren(layerList, "Layer")) {
            // get layer name
            Element nameElem = DOMUtils.getChild(elem, "Name");
            String name = DOMUtils.getContent(nameElem);
            // is layer in list of requested layers?
            if (!contains(layers, name))
                continue;
            // is layer visible?
            if (Coerce.toBoolean(elem.getAttribute("hidden")))
                continue;
            // get href to layer's service
            Node server = DOMUtils.getChild(elem, "Server");
            Element or = DOMUtils.getChild(server, "OnlineResource");
            String href = or.getAttribute("xlink:href");

            if (href.equals(lastServer)) {
                String layerNames = requests.remove(requests.size() - 1);
                layerNames += "," + name;
                requests.add(layerNames);
            } else {
                lastServer = href;
                requests.add(getMapRequest(href, name, bbox, width, height, format));
            }
        }
        return requests;
    }

    /**
     * Determines if the <code>array</code> contains the <code>item</code>.
     * 
     * @param item String name
     * @param array String[]
     * @return true if the array is empty or null or if the item is within the array
     * @ssdd
     */
    private boolean contains(String[] array, String item) {

        if (array == null || array.length == 0)
            return true;
        for (String arrayItem : array) {
            if (arrayItem.equals(item)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Composes a GetMap request url for a single layer.
     * 
     * @param href String url to the layer's WMS
     * @param name String id of the layer
     * @param bbox Envelope spatial extent of the map image
     * @param width x-dimension of the map image
     * @param height y-dimension of the map image
     * @param format String mime-type of the map image
     * @return String url for a GetMap request to the layer's WMS
     * @ssdd
     */
    protected String getMapRequest(String href,
                                   String name,
                                   Envelope bbox,
                                   int width,
                                   int height,
                                   String format) {

        String url = href.contains("?") ? (!href.endsWith("?") ? href + "&" : href + "") : href +
                                                                                           "?";
        url += "request=GetMap&service=WMS&version=1.1.1";
        url += "&bbox=" + bbox.getMinX() + "," + bbox.getMinY() + "," + bbox.getMaxX() + "," +
               bbox.getMaxY();
        url += "&width=" + width;
        url += "&height=" + height;
        url += "&format=" + format;
        url += "&Transparent=true";
        url += "&SRS=EPSG:4326";
        url += "&layers=" + name;
        return url;
    }

    /**
     * This method retrieves a remote image and renders it into the graphics context.
     * 
     * @param url String url for retrieving an image
     * @param image Image
     * @ssdd
     */
    private void fetchImage(String url, Graphics2D graphics) {

        if (log.isDebugEnabled()) {
            log.debug("Fetching image from: " + url);
        }

        try {
            // BufferedImage layer = ImageIO.read(new URL(url));
            BufferedImage layer = toBufferedImage(Toolkit.getDefaultToolkit().createImage(new URL(url)));
            // if(log.isDebugEnabled()) {
            // String f = format.substring(format.indexOf("/")+1, format.length());
            // InputStream in = new URL(url).openStream();
            // IOUtils.copy(in, new FileOutputStream("target/" + name + ".in." + f));
            // Image iolayer = Toolkit.getDefaultToolkit().createImage(new URL(url));
            // ImageIO.write(toBufferedImage(iolayer), f, new File("target/" + name + ".io." + f));
            // in.close();
            // }
            if (layer != null) {
                AffineTransform identity = new AffineTransform();
                graphics.setTransform(identity);
                graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
                graphics.drawImage(layer, identity, null);
            }
        } catch (Throwable e) {
            log.error("Error retrieving image", e);
        }
    }

    /**
     * Create a <code>BufferedImage</code> with the specified dimensions. If the format is "gif"
     * then the image will be cleared with the background color (black).
     * 
     * @param format String
     * @param width int
     * @param height int
     * @param transparent boolean
     * @return BufferedImage
     * @ssdd
     */
    public static BufferedImage createImage(String format,
                                            int width,
                                            int height,
                                            boolean transparent) {

        BufferedImage image;
        int imageType = transparent ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        image = new BufferedImage(width, height, imageType);
        if ("gif".equals(format)) {
            // clear the background with transparent black.
            Graphics2D graphics = (Graphics2D) image.getGraphics();
            graphics.setBackground(BACKGROUND_COLOR);
            graphics.clearRect(0, 0, width, height);
        }
        return image;
    }

    /**
     * This method returns a buffered image with the contents of an image.
     * 
     * Borrowed and modified from the Java Developer's Almanac:
     * http://www.exampledepot.com/egs/java.awt.image/Image2Buf.html
     * 
     * @param image
     * @return bufferedImage
     * @ssdd
     */
    public static BufferedImage toBufferedImage(Image image) {

        if (image instanceof BufferedImage)
            return (BufferedImage) image;

        // This code ensures that all the pixels in the image are loaded
        image = new ImageIcon(image).getImage();

        int width = image.getWidth(null);
        int height = image.getHeight(null);
        if (width <= 0 || height <= 0)
            return null;

        // Determine if the image has transparent pixels; for this method's
        // implementation, see e661 Determining If an Image Has Transparent Pixels
        boolean hasAlpha = hasAlpha(image);

        // Create a buffered image with a format that's compatible with the screen
        BufferedImage bimage = null;
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        try {
            // Determine the type of transparency of the new buffered image
            int transparency = hasAlpha ? Transparency.BITMASK : Transparency.OPAQUE;

            // Create the buffered image
            GraphicsDevice gs = ge.getDefaultScreenDevice();
            GraphicsConfiguration gc = gs.getDefaultConfiguration();
            bimage = gc.createCompatibleImage(width, height, transparency);
        } catch (HeadlessException e) {
            // The system does not have a screen
        }

        if (bimage == null) {
            // Create a buffered image using the default color model
            int type = hasAlpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
            bimage = new BufferedImage(image.getWidth(null), image.getHeight(null), type);
        }
        Graphics2D g = bimage.createGraphics(); // Copy image to buffered image
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC, 1.f));
        g.drawImage(image, 0, 0, null); // Paint the image onto the buffered image
        g.dispose();
        return bimage;
    }

    /**
     * This method returns true if the specified image has transparent pixels
     * 
     * Borrowed from the Java Developer's Almanac:
     * http://www.exampledepot.com/egs/java.awt.image/HasAlpha.html
     * 
     * @param image
     * @return boolean
     * @ssdd
     */
    public static boolean hasAlpha(Image image) {

        // // If buffered image, the color model is readily available
        // if (image instanceof BufferedImage) {
        // BufferedImage bimage = (BufferedImage)image;
        // return bimage.getColorModel().hasAlpha();
        // }
        //
        // // Use a pixel grabber to retrieve the image's color model;
        // // grabbing a single pixel is usually sufficient
        // PixelGrabber pg = new PixelGrabber(image, 0, 0, 1, 1, false);
        // try {
        // pg.grabPixels();
        // } catch (InterruptedException e) {
        // }
        //
        // // Get the image's color model
        // ColorModel cm = pg.getColorModel();
        // return cm.hasAlpha();
        return true;
    }
}
