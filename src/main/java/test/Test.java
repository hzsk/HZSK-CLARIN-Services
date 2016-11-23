/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.exmaralda.common.corpusbuild.TEIMerger;
import org.exmaralda.exakt.utilities.FileIO;
import org.exmaralda.partitureditor.jexmaralda.BasicTranscription;
import org.exmaralda.partitureditor.jexmaralda.SegmentedTranscription;
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
 *
 * @author Thomas_Schmidt
 */
public class Test {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            new Test().doit();
        } catch (Exception ex) {
            Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void doit() throws Exception {
        BasicTranscription exb = new BasicTranscription();
        segment(exb, "HIAT", "DE");
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
        textElement.setAttribute("xml:lang", language);
                
        return teiDoc;
    }
    
    
}
