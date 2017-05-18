/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package converters;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import org.exmaralda.common.corpusbuild.TEIMerger;
import org.exmaralda.common.jdomutilities.IOUtilities;
import org.exmaralda.exakt.utilities.FileIO;
import org.exmaralda.partitureditor.jexmaralda.BasicTranscription;
import org.exmaralda.partitureditor.jexmaralda.JexmaraldaException;
import org.exmaralda.partitureditor.jexmaralda.SegmentedTranscription;
import org.exmaralda.partitureditor.jexmaralda.convert.CHATConverter;
import org.exmaralda.partitureditor.jexmaralda.convert.StylesheetFactory;
import org.exmaralda.partitureditor.jexmaralda.convert.TEITCFMerger;
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
import org.xml.sax.SAXException;

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
    /****************     EXMARaLDA(EXB) ==> ISO/TEI       *****************/
    /***********************************************************************/

    /**
     * Retrieves representation of an instance of converters.IsoTeiConverter
     * @param sourceData
     * @param segmentationAlgorithm
     * @param language
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Path("/exb2isoTeiConverter")
    @Consumes("application/xml;format-variant=exmaralda-exb")
    @Produces("application/tei+xml;format-variant=tei-iso-spoken;tokenized=1")    
    public Response exb2isoTei(InputStream sourceData, 
            @QueryParam("seg") String segmentationAlgorithm,
            @QueryParam("lang") String language
    ) {
        try {
            System.out.println("##### EXB2TEI: Method entered");
            //read exb from input stream
            Scanner s = new Scanner(sourceData).useDelimiter("\\A");
            String inputXml = s.hasNext() ? s.next() : "";                    
            BasicTranscription exb = new BasicTranscription();
            exb.BasicTranscriptionFromString(inputXml);
            
            Document teiDoc = segment(exb, segmentationAlgorithm, language);

            // generate IDs for all elements in the TEI document
            generateWordIDs(teiDoc);

            String isoTeiResultString = IOUtilities.documentToString(teiDoc);
            
            System.out.println("##### EXB2TEI: Result string was generated");

            Response buildResponse = Response.ok(isoTeiResultString).build();
            return buildResponse;
        } catch (SAXException e) {
            throw new WebApplicationException(e, Response
                    .status(400).entity(e.getStackTrace()).build());             
        } catch (JexmaraldaException e) {
            throw new WebApplicationException(e, Response
                    .status(400).entity(e.getStackTrace()).build());             
        } catch (IOException e) {
            throw new WebApplicationException(e, Response
                    .status(400).entity(e.getStackTrace()).build());             
        } catch (Exception e) {
            throw new WebApplicationException(e, Response
                    .status(400).entity(e.getStackTrace()).build());             
        }
    }
    
    /***********************************************************************/
    /************************ FOLKER ==> ISO/TEI ***************************/
    /***********************************************************************/

    static String FOLKER2ISOTEI_XSL = "/org/exmaralda/tei/xml/folker2isotei.xsl";
    
    /**
     * Retrieves representation of an instance of converters.IsoTeiConverter
     * @param sourceData
     * @param language
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Path("/fln2isoTeiConverter")
    @Consumes("application/xml;format-variant=folker-fln")
    @Produces("application/tei+xml;format-variant=tei-iso-spoken;tokenized=1")    
    public Response fln2isoTei(
            InputStream sourceData,
            @QueryParam("lang") String language) {
        try{
            //read fln from input stream
            Scanner s = new Scanner(sourceData).useDelimiter("\\A");
            String inputXml = s.hasNext() ? s.next() : "";                    
            
            // initiate an XSL engine capable of XSL 2
            StylesheetFactory sf = new StylesheetFactory(true);
            // Define parameters for the stylesheet transformation
            String[][] parameters = {
                {"LANGUAGE", language}
            };
            // do the stylesheet transformation
            String firstResult = sf.applyInternalStylesheetToString(FOLKER2ISOTEI_XSL, inputXml, parameters);
            
            Document teiDoc = IOUtilities.readDocumentFromString(firstResult);
            // generate IDs for all elements in the TEI document
            generateWordIDs(teiDoc);

            String isoTeiResultString = IOUtilities.documentToString(teiDoc);
            System.out.println("##### FLN2TEI: Result string was generated");
            return Response.ok(isoTeiResultString).build();
        } catch (SAXException | ParserConfigurationException | IOException | TransformerException | JDOMException e){
            throw new WebApplicationException(e, Response
                    .status(400).entity(e.getStackTrace()).build());             
        }
    }
    
    /***********************************************************************/
    /****************     Transcriber ==> ISO/TEI    ***********************/
    /***********************************************************************/

    /**
     * Retrieves representation of an instance of converters.IsoTeiConverter
     * @param sourceData
     * @param segmentationAlgorithm
     * @param language
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Path("/transcriber2isoTeiConverter")
    @Consumes("application/xml;format-variant=transcriber-trs")
    @Produces("application/tei+xml;format-variant=tei-iso-spoken;tokenized=1")    
    public Response transcriber2isoTei(InputStream sourceData, 
            @QueryParam("seg") String segmentationAlgorithm,
            @QueryParam("lang") String language
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
            
            Document teiDoc = segment(exb, segmentationAlgorithm, language);

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
    /************************    CHAT ==> ISO/TEI     **********************/
    /***********************************************************************/

    /**
     * Retrieves representation of an instance of converters.IsoTeiConverter
     * @param sourceData
     * @param segmentationAlgorithm
     * @param language
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Path("/chat2isoTeiConverter")
    @Consumes("text/plain;format-variant=clan-cha")     
    @Produces("application/tei+xml;format-variant=tei-iso-spoken;tokenized=1")    
    public Response chat2isoTei(InputStream sourceData, 
            @QueryParam("seg") String segmentationAlgorithm,
            @QueryParam("lang") String language            
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
            
            Document teiDoc = segment(exb, segmentationAlgorithm, language);

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
    /************************    ISO/TEI ==> TCF      **********************/
    /***********************************************************************/
    
    static String ISOTEI2TCF_XSL = "/org/exmaralda/partitureditor/jexmaralda/xsl/ISOTEI2TCF.xsl";

    /**
     * Retrieves representation of an instance of converters.IsoTeiConverter
     * @param sourceData
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Path("/isoTei2TcfConverter")
    @Consumes("application/tei+xml;format-variant=tei-iso-spoken;tokenized=1")
    @Produces("text/tcf+xml")
    /* @Produces("application/xml;format-variant=weblicht-tcf")    */
    public Response isoTei2Tcf(InputStream sourceData) {
        try{
            //read ISO/TEI from input stream
            Scanner s = new Scanner(sourceData).useDelimiter("\\A");
            String inputXml = s.hasNext() ? s.next() : "";   
            // initiate an XSL engine capable of XSL 2
            StylesheetFactory sf = new StylesheetFactory(true);
            // do the stylesheet transformation
            String tcfResultString = sf.applyInternalStylesheetToString(ISOTEI2TCF_XSL, inputXml);            
            return Response.ok(tcfResultString).build();
        } catch (SAXException | ParserConfigurationException | IOException | TransformerException e){
            throw new WebApplicationException(e, Response
                    .status(400).entity(e.getStackTrace()).build());             
        }
    }

    /***********************************************************************/
    /************************    ISO/TEI ==> TCF      **********************/
    /***********************************************************************/
    

    /**
     * Retrieves representation of an instance of converters.IsoTeiConverter
     * @param sourceData
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Path("/tcf2isoTeiConverter")
    @Consumes("text/tcf+xml")    
    /* @Consumes("application/xml;format-variant=weblicht-tcf")    */
    @Produces("application/tei+xml;format-variant=tei-iso-spoken;tokenized=1")
    public Response tcf2isoTei(InputStream sourceData) {
        try{
            //read TCF from input stream
            Scanner s = new Scanner(sourceData).useDelimiter("\\A");
            String inputXml = s.hasNext() ? s.next() : "";   
            // write a temp file
            File tcfResultFile = File.createTempFile("TCF", ".tcf");
            tcfResultFile.deleteOnExit();
            FileIO.writeDocumentToLocalFile(tcfResultFile, IOUtilities.readDocumentFromString(inputXml));
            
            // merge TCF into original TEI (included in textSource) 
            TEITCFMerger merger = new TEITCFMerger(tcfResultFile);            
            merger.merge();            
            Document mergedDocument = merger.getMergedDocument();
            
            String isoTeiResultString = IOUtilities.documentToString(mergedDocument);
            return Response.ok(isoTeiResultString).build();
        } catch (IOException | JDOMException e){
            throw new WebApplicationException(e, Response
                    .status(400).entity(e.getStackTrace()).build());             
        } 
    }
    
    /**
     * PUT method for updating or creating an instance of IsoTeiConverter
     * @param content representation for the resource
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

    private Document segment(BasicTranscription exb, String segmentationAlgorithm, String language) throws XSLTransformException, JDOMException, IOException, Exception {
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

        // added 08-07-2016: need to set the language in the TEI document
        XPath textXPath = XPath.newInstance("//tei:text[1]"); 
        textXPath.addNamespace("tei", "http://www.tei-c.org/ns/1.0");
        textXPath.addNamespace(Namespace.XML_NAMESPACE);        
        Element textElement = (Element)(textXPath.selectSingleNode(teiDoc));
        //textElement.setAttribute("xml:lang", language);
        textElement.setAttribute("lang", language, Namespace.XML_NAMESPACE);
                
        return teiDoc;
    }
    
}
