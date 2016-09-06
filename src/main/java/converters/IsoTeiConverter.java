/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package converters;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.HttpsURLConnection;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.POST;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import static javax.ws.rs.core.HttpHeaders.USER_AGENT;
import javax.ws.rs.core.Response;
import org.exmaralda.common.corpusbuild.TEIMerger;
import org.exmaralda.common.jdomutilities.IOUtilities;
import org.exmaralda.exakt.utilities.FileIO;
import org.exmaralda.partitureditor.jexmaralda.BasicTranscription;
import org.exmaralda.partitureditor.jexmaralda.SegmentedTranscription;
import org.exmaralda.partitureditor.jexmaralda.convert.CHATConverter;
import org.exmaralda.partitureditor.jexmaralda.convert.StylesheetFactory;
import org.exmaralda.partitureditor.jexmaralda.convert.TranscriberConverter;
import org.exmaralda.partitureditor.jexmaralda.segment.AbstractSegmentation;
import org.exmaralda.partitureditor.jexmaralda.segment.CHATSegmentation;
import org.exmaralda.partitureditor.jexmaralda.segment.GenericSegmentation;
import org.exmaralda.partitureditor.jexmaralda.segment.HIATSegmentation;
import org.exmaralda.partitureditor.jexmaralda.segment.cGATMinimalSegmentation;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.transform.XSLTransformException;
import org.jdom.xpath.XPath;

/**
 * REST Web Service
 *
 * @author Schmidt
 */
@Path("")
public class IsoTeiConverter {

    @Context
    private UriInfo context;

    /**
     * Creates a new instance of IsoTeiConverter
     */
    public IsoTeiConverter() {
    }

    /***********************************************************************/
    /************************     EXMARaLDA       **************************/
    /***********************************************************************/

    public String exb2isoTei(InputStream sourceData, String segmentationAlgorithm
    ) {
        String result = "";
        
        try{
            //read exb from input stream
            Scanner s = new Scanner(sourceData).useDelimiter("\\A");
            String inputXml = s.hasNext() ? s.next() : "";                    
            BasicTranscription exb = new BasicTranscription();
            exb.BasicTranscriptionFromString(inputXml);
            
            Document teiDoc = segment(exb, segmentationAlgorithm);

            // generate IDs for all elements in the TEI document
            generateWordIDs(teiDoc);

            result = IOUtilities.documentToString(teiDoc);
        } catch (Exception e){
            e.printStackTrace();       
        }
        
        return result;
    }
    
    /**
     * Retrieves representation of an instance of converters.IsoTeiConverter
     * @param sourceData
     * @param segmentationAlgorithm
     * @return an instance of java.lang.String
     */
    @POST
    @Path("/exb2isoTeiConverter")
    @Consumes("text/x-exmaralda-exb+xml")
    @Produces("text/x-iso-tei-spoken+xml")    
    public Response exb2isoTeiPost(InputStream sourceData, 
            @QueryParam("seg") String segmentationAlgorithm
    ) {
        try{
            String isoTeiResultString = this.exb2isoTei(sourceData, segmentationAlgorithm);
            return Response.ok(isoTeiResultString).build();
        } catch (Exception e){
            throw new WebApplicationException(e, Response
                    .status(400).entity(e.getStackTrace()).build());             
        }
    }
    
    
    /**
     * Retrieves representation of an instance of converters.IsoTeiConverter
     * @param sourceData
     * @param segmentationAlgorithm
     * @return an instance of java.lang.String
     */
    @POST
    @Path("/exb2isoTeiConverter/{segAlgorithm}")
    @Consumes("text/x-exmaralda-exb+xml")
    @Produces("text/x-iso-tei-spoken+xml")    
    public Response exb2isoTeiPath(InputStream sourceData, 
            @PathParam("segAlgorithm") String segmentationAlgorithm
    ) {
        try{
            String isoTeiResultString = this.exb2isoTei(sourceData, segmentationAlgorithm);
            return Response.ok(isoTeiResultString).build();
        } catch (Exception e){
            throw new WebApplicationException(e, Response
                    .status(400).entity(e.getStackTrace()).build());             
        }
    }
    
       
    /***********************************************************************/
    /************************     FOLKER          **************************/
    /***********************************************************************/

    static String FOLKER2ISOTEI_XSL = "/org/exmaralda/tei/xml/folker2isotei.xsl";
    
    /**
     * Retrieves representation of an instance of converters.IsoTeiConverter
     * @param sourceData
     * @return an instance of java.lang.String
     */
    @POST
    @Path("/fln2isoTeiConverter")
    @Consumes("text/x-exmaralda-fln+xml")
    @Produces("text/x-iso-tei-spoken+xml")    
    public Response fln2isoTei(InputStream sourceData) {
        try{
            //read fln from input stream
            Scanner s = new Scanner(sourceData).useDelimiter("\\A");
            String inputXml = s.hasNext() ? s.next() : "";                    
            
            // initiate an XSL engine capable of XSL 2
            StylesheetFactory sf = new StylesheetFactory(true);
            // do the stylesheet transformation
            String firstResult = sf.applyInternalStylesheetToString(FOLKER2ISOTEI_XSL, inputXml);
            
            Document teiDoc = IOUtilities.readDocumentFromString(firstResult);
            // generate IDs for all elements in the TEI document
            generateWordIDs(teiDoc);

            String isoTeiResultString = IOUtilities.documentToString(teiDoc);
            return Response.ok(isoTeiResultString).build();
        } catch (Exception e){
            throw new WebApplicationException(e, Response
                    .status(400).entity(e.getStackTrace()).build());             
        }
    }
    
    /***********************************************************************/
    /************************     Transcriber     **************************/
    /***********************************************************************/

    /**
     * Retrieves representation of an instance of converters.IsoTeiConverter
     * @param sourceData
     * @param segmentationAlgorithm
     * @return an instance of java.lang.String
     */
    @POST
    @Path("/transcriber2isoTeiConverter")
    @Consumes("text/x-transcriber-trs+xml")
    @Produces("text/x-iso-tei-spoken+xml")    
    public Response transcriber2isoTei(InputStream sourceData, 
            @QueryParam("seg") String segmentationAlgorithm
    ) {
        try{
            //read trs from input stream
            Scanner s = new Scanner(sourceData).useDelimiter("\\A");
            String inputXml = s.hasNext() ? s.next() : "";                    
            
            // create a temp file on the server
            // (because Transcriber Converter cannot read from string...)
            File tempFile = File.createTempFile("TRANSCRIBER", ".TRS");
            tempFile.deleteOnExit();
            Document trsDoc = IOUtilities.readDocumentFromString(inputXml);
            FileIO.writeDocumentToLocalFile(tempFile, trsDoc);
            
            TranscriberConverter transcriberConverter = new TranscriberConverter();
            BasicTranscription exb = transcriberConverter.readTranscriberFromFile(tempFile.getAbsolutePath());
            
            Document teiDoc = segment(exb, segmentationAlgorithm);

            // generate IDs for all elements in the TEI document
            generateWordIDs(teiDoc);

            String isoTeiResultString = IOUtilities.documentToString(teiDoc);

            return Response.ok(isoTeiResultString).build();
        } catch (Exception e){
            throw new WebApplicationException(e, Response
                    .status(400).entity(e.getStackTrace()).build());             
        }
    }
    
    /***********************************************************************/
    /************************     CHAT            **************************/
    /***********************************************************************/

    /**
     * Retrieves representation of an instance of converters.IsoTeiConverter
     * @param sourceData
     * @param segmentationAlgorithm
     * @return an instance of java.lang.String
     */
    @POST
    @Path("/chat2isoTeiConverter")
    @Consumes("text/x-cha+txt")     // oder gibt es schon einen MIME-Type f�r CHAT-Daten?
    @Produces("text/x-iso-tei-spoken+xml")    
    public Response chat2isoTei(InputStream sourceData, 
            @QueryParam("seg") String segmentationAlgorithm
    ) {
        try{
            //read cha from input stream
            Scanner s = new Scanner(sourceData).useDelimiter("\\A");
            // I smell an encoding problem here... 
            String chatText = s.hasNext() ? s.next() : "";                    
            
            // create a temp file on the server
            // (because Chat Converter cannot read from string...)
            File tempFile = File.createTempFile("CHAT", ".CHA");
            tempFile.deleteOnExit();
            FileOutputStream fos = new FileOutputStream(tempFile);
            fos.write(chatText.getBytes("UTF-8"));
            fos.close();
            
            
            CHATConverter chatConverter = new CHATConverter(tempFile);
            BasicTranscription exb = chatConverter.convert();
            
            Document teiDoc = segment(exb, segmentationAlgorithm);

            // generate IDs for all elements in the TEI document
            generateWordIDs(teiDoc);

            String isoTeiResultString = IOUtilities.documentToString(teiDoc);

            return Response.ok(isoTeiResultString).build();
        } catch (Exception e){
            throw new WebApplicationException(e, Response
                    .status(400).entity(e.getStackTrace()).build());             
        }
    }

    /**
     * PUT method for updating or creating an instance of IsoTeiConverter
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @PUT
    @Consumes("application/xml")
    public void putXml(String content) {
    }
    
    //****************************************************
    //********* private processing methods ***************
    //****************************************************

    // new 30-03-2016
    private void generateWordIDs(Document document) throws JDOMException{
        // added 30-03-2016
        HashSet<String> allExistingIDs = new HashSet<String>();
        XPath idXPath = XPath.newInstance("//tei:*[@xml:id]"); 
        idXPath.addNamespace("tei", "http://www.tei-c.org/ns/1.0");
        idXPath.addNamespace(Namespace.XML_NAMESPACE);
        List idElements = idXPath.selectNodes(document);
        for (Object o : idElements){
            Element e = (Element)o;
            allExistingIDs.add(e.getAttributeValue("id", Namespace.XML_NAMESPACE));
        }
        
        
        // changed 30-03-2016
        XPath wordXPath = XPath.newInstance("//tei:w[not(@xml:id)]"); 
        wordXPath.addNamespace("tei", "http://www.tei-c.org/ns/1.0");
        wordXPath.addNamespace(Namespace.XML_NAMESPACE);
        
        List words = wordXPath.selectNodes(document);
        int count=1;
        for (Object o : words){
            Element word = (Element)o;
            while(allExistingIDs.contains("w" + Integer.toString(count))){
                count++;
            }

            String wordID = "w" + Integer.toString(count);
            allExistingIDs.add(wordID);
            //System.out.println("*** " + wordID);
            word.setAttribute("id", wordID, Namespace.XML_NAMESPACE);
        }
        
        // new 02-12-2014
        XPath pcXPath = XPath.newInstance("//tei:pc[not(@xml:id)]"); 
        pcXPath.addNamespace("tei", "http://www.tei-c.org/ns/1.0");
        pcXPath.addNamespace(Namespace.XML_NAMESPACE);
        
        List pcs = pcXPath.selectNodes(document);
        count=1;
        for (Object o : pcs){
            Element pc = (Element)o;
            while(allExistingIDs.contains("pc" + Integer.toString(count))){
                count++;
            }
            
            String pcID = "pc" + Integer.toString(count);
            allExistingIDs.add(pcID);
            //System.out.println("*** " + wordID);
            pc.setAttribute("id", pcID, Namespace.XML_NAMESPACE);
        }
    }

    private Document segment(BasicTranscription exb, String segmentationAlgorithm) throws XSLTransformException, JDOMException, IOException, Exception {
        // pick the right segmentation algorithm 
        // according to the parameter "seg" passed to the service
        AbstractSegmentation segmentation = new GenericSegmentation();
        if ("HIAT".equals(segmentationAlgorithm)){
            segmentation = new HIATSegmentation();
        } else if ("cGAT Minimal".equals(segmentationAlgorithm)){
            segmentation = new cGATMinimalSegmentation();                                                                      
        } else if ("CHAT".equals(segmentationAlgorithm)){
            segmentation = new CHATSegmentation();
        }

        // do the segmentation
        SegmentedTranscription st = segmentation.BasicToSegmented(exb);

        // merge event and linguistic segmentation into one
        // TEI file
        String nameOfDeepSegmentation = "SpeakerContribution_Word";
        if ("HIAT".equals(segmentationAlgorithm)){
            nameOfDeepSegmentation = "SpeakerContribution_Utterance_Word";
        } else if ("CHAT".equals(segmentationAlgorithm)){
            nameOfDeepSegmentation = "SpeakerContribution_Utterance_Word";            
        }
        TEIMerger teiMerger = new TEIMerger(true);
        Document stdoc = FileIO.readDocumentFromString(st.toXML());
        Document teiDoc = teiMerger.SegmentedTranscriptionToTEITranscription(stdoc, nameOfDeepSegmentation, "SpeakerContribution_Event", true);
        return teiDoc;
    }
    
}
