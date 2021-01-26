package com.example.datanarratives;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
//import org.json.simple.parser.JSONParser;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class DataNarratives {
    private static String dataQueryNarrative(String dataQuery) {
//        System.out.println("\n"+dataQuery);
        String dataQuery1 = dataQuery.replaceAll("^(//)n${1}",""); //this is necessary to replace the new line characters in query
        String[] querylist = dataQuery1.split("\\.");
        String rdfs_label = "rdfs:label";
        //storing properties
        Map<String, String> properties = new HashMap<>();
        for(int i = 0; i < querylist.length; i++) {
//            System.out.println("\n"+ querylist[i]);
            if(querylist[i].contains(rdfs_label)){
                String[] line = querylist[i].replace("\\","").split(" ");
                properties.put(line[2],line[4].replace('"',' '));
            }
        }

        //We map all the objects to the properties they were identified with, by using the objects dictionary
        Map<String, ArrayList> inputs = new HashMap<>();
        Map<ArrayList, String> outputs = new HashMap<>();
        for(int i = 0; i < querylist.length; i++) {
            if(!querylist[i].contains(rdfs_label)){
                String[] line = querylist[i].split(" ");
                String schema = "Schema";
                if(!inputs.containsKey(line[2])&!line[2].contains(schema)){
                    ArrayList<ArrayList> list = new ArrayList<>();
                    ArrayList<String> item = new ArrayList<>();
                    item.add(line[2]);
                    item.add(properties.get(line[3]));
//                    item.add(line[4]);
                    list.add(item);
                    inputs.put(line[2],list);
                    outputs.put(item,line[4]);
                }
                else if(inputs.containsKey(line[2])&!line[2].contains(schema)){
                    ArrayList list2 = inputs.get(line[2]);
                    ArrayList item = new ArrayList();
                    item.add(line[2]);
                    item.add(properties.get(line[3]));
                    list2.add(item);
                    inputs.put(line[2],list2);
                    ArrayList<String> list = new ArrayList<>();
                    list.add(line[2]);
                    list.add(properties.get(line[3]));
                    outputs.put(item,line[4]);
                }
            }
        }

        //Now we traverse the path
        String path = "";
        for (Map.Entry mapElement : inputs.entrySet()) {
            String key = (String)mapElement.getKey();
            ArrayList value = (ArrayList) mapElement.getValue();
            for(int j=0;j<value.size();j++){
                //p = v
                List p = (List) value.get(j);
                try {
                    path = path+key.replace("?","")+"->"+ p.get(1).toString().trim().replace("?","") +"->"+outputs.get(p).toString().replace("?","")+"\n";
                }catch (NullPointerException e){

                }
                }
            }
//        System.out.println("Narrative"+path);
        return path;
    }

    private static JsonArray getJsonArray(String string_url) throws IOException {
        //This function can be used to retrieve the execution data as a JSON array, given the url
        URL url = new URL(string_url);
        URLConnection request = url.openConnection();
        request.connect();
        JsonParser jp = new JsonParser();
        JsonElement root = jp.parse(new InputStreamReader((InputStream) request.getContent()));
        JsonArray jsonArray1 = root.getAsJsonArray();
        return jsonArray1;
    }
    

    private static String evolvingHypothesisNarrative(String curr_url, String old_url) throws IOException {
        JsonArray currJson = getJsonArray(curr_url);
        JsonArray prevJson = getJsonArray(old_url);
        Map<String, String> curr_template = getTemplate(currJson);
        Map<String, String> prev_template = getTemplate(prevJson);
        //We want to store all the data relevant to the evolving hypothesis in a new hashmap
        Map<String, String> hyp_template = new HashMap<>();
        hyp_template.put("LOI",curr_template.get("LOIName"));
        hyp_template.put("Hypothesis",curr_template.get("HypothesisName"));
        hyp_template.put("currDatasets",curr_template.get("Datasets"));
        int currDatasetsCount = curr_template.get("Datasets").split(",").length;
        int len1 = curr_template.get("DateCreated").length();
        hyp_template.put("currDate",curr_template.get("DateCreated").split(" ")[1]);
        hyp_template.put("curr_pvalue",curr_template.get("pvalue"));

        hyp_template.put("prevDatasets",prev_template.get("Datasets"));
        int prevDatasetsCount = prev_template.get("Datasets").split(",").length;
        int len2 = prev_template.get("DateCreated").length();
        hyp_template.put("prevDate",prev_template.get("DateCreated").split(" ")[1]);
        hyp_template.put("prev_pvalue",prev_template.get("pvalue"));
        int difference = currDatasetsCount - prevDatasetsCount;
        hyp_template.put("addedDatasets",String.valueOf(difference));

        String hypothesisNarrative = "The hypothesis with the title: "+hyp_template.get("Hypothesis")+", was triggered on the DISK portal, on "+hyp_template.get("currDate")+". It was previously triggered on "+hyp_template.get("prevDate")+" and tested with "+String.valueOf(prevDatasetsCount)+" additional datasets, resulting in a p-value of "+hyp_template.get("prev_pvalue")+". As of "+hyp_template.get("currDate")+", it has been tested with "+hyp_template.get("addedDatasets")+" datasets, resulting in an updated p-value of "+hyp_template.get("curr_pvalue")+".";
        return hypothesisNarrative;

    }

    private static String executionNarrative(Map template_data){
        String execNarrative = "Execution Narrative:\nThe Hypothesis with title: "+template_data.get("HypothesisName")+" was tested "+template_data.get("Status")+" with the Line of Inquiry: "+template_data.get("LOIName")+"("+template_data.get("LOILink")+"). The LOI triggered the workflow on WINGS("+template_data.get("WorkflowLink")+") where it was tested with the following datasets: "+template_data.get("Datasets")+". The resulting confidence value is "+template_data.get("pvalue")+".";
        return execNarrative;
    }


    private static Map<String, String> getTemplate(JsonArray jsonArray){
        Map<String, Map<String, String>> map = new HashMap<>(); //we want to store the relevant info in the map
        for (int i = 0; i < jsonArray.size(); i++) {
            JsonElement element = jsonArray.get(i);
            JsonObject obj = element.getAsJsonObject();
            JsonElement subject_element = obj.get("subject");
            JsonElement property_element = obj.get("predicate");
            JsonElement object_element = obj.get("object");
            JsonObject values = object_element.getAsJsonObject();
            JsonElement value_element = values.get("value");
            String prop = property_element.getAsString();
            String value = value_element.getAsString();
            String sub = subject_element.getAsString();
            // We want to store the objects and properties as strings, not URIs
            String[] propertylist = prop.split("#");
            String property = propertylist[propertylist.length - 1];
            String[] subjectlist = sub.split("/");
            String subject = subjectlist[subjectlist.length - 1];
            if (map.containsKey(subject)) {
                HashMap list = (HashMap) map.get(subject);
                list.put(property, value);
                map.put(subject,list);
            } else {
                HashMap list = new HashMap();
                list.put(property, value);
                map.put(subject, list);
            }
        }

        //We iterate through this map to obtain more specific information related to the LOI

        Map<String, String> template_data = new HashMap<>();
//        System.out.println("MAP KEYS MAP KEYS"+map.keySet());
        for (String name : map.keySet()) {
            if (name.startsWith("LOI")) {
//                System.out.println("LOI:"+map.get(name));
                Map<String, String> LOI = map.get(name);
                //Iterating through hashMap of properties related to LOI
                template_data.put("DataQuery",LOI.get("hasDataQuery"));
                //Adding date created
//                template_data.put("DateCreated",LOI.get("dateCreated"));
//                //Adding Hypothesis
                template_data.put("LOIName",LOI.get("comment"));
            }
            else if (name.startsWith("Hypothesis")){
                Map<String, String> Hyp = map.get(name);
                if(Hyp.get("label")!=null){
                    template_data.put("HypothesisName",Hyp.get("label"));
                }

            }
            else if (name.startsWith("TriggeredLOI")){
                Map<String, String> TLOI = map.get(name);
                //Iterating through properties related to TLOI
                //Adding status
                String item4 = TLOI.get("hasTriggeredLineOfInquiryStatus");
                List temp_list4 = new ArrayList();
                String LOI_status;
                if(item4.startsWith("SUCCESSFUL")){
                    LOI_status="successfully";
                }
                else{
                    LOI_status="unsuccessfully";
                }
                template_data.put("Status",LOI_status);

                //Adding LOI Link
                template_data.put("LOILink",TLOI.get("hasLineOfInquiry"));
                template_data.put("DateCreated",TLOI.get("dateCreated"));

                //Adding Resulting Hypothesis Link
                template_data.put("ResultHypothesisLink",TLOI.get("hasResultingHypothesis"));
                //Getting hypothesis name
                String hypLink = TLOI.get("hasParentHypothesis");
            }

            else{
                //if the item is not an LOI or TLOI reference
                Map<String, String> curr = map.get(name);
                //Retrieve workflow link
                try{
                    String workflow = curr.get("hasWorkflow");
                    if(workflow.startsWith("http")){
                        template_data.put("WorkflowLink",workflow);
                    }
                } catch (NullPointerException e){
                }

                //Retrieve confidence value
                try{
                    String pvalue1 = curr.get("hasConfidenceValue");
                    if(!pvalue1.startsWith("null")){
                        Float pvalue = Float.parseFloat(pvalue1);
                        pvalue = (float)Math.round(pvalue * 100f) / 100f;
                        template_data.put("pvalue",pvalue.toString());
//                        System.out.println("\n\npvalue"+pvalue);
                    }
                } catch (NullPointerException e){
                }
                //Retrieve datasets
                try {
                    String datasets = curr.get("hasBindingValue");
                    String variable = curr.get("hasVariable");
                    String type = curr.get("type");
                    if (datasets.startsWith("?")) {
//                        continue;
                    }
                    if(datasets.startsWith("\\")){
//                        continue;
                    }
                    if(datasets.startsWith("[")){
                        template_data.put("Datasets", datasets);
//                        System.out.println("Datasets:" + datasets);
                    }

                } catch (NullPointerException e){
                    continue;
                }

//                }
            }
        }
        return template_data;
    }


    public static void main(String[] args) throws IOException {
        String prev_url = "https://enigma-disk.wings.isi.edu/disk-server/admin/test/triples/TriggeredLOI-ZwuQXhxbW5hK";
        String surl = "https://enigma-disk.wings.isi.edu/disk-server/admin/test/triples/TriggeredLOI-bFW4E5PQSJCN";
        JsonArray jsonArray = getJsonArray(surl);
        Map<String, String> template_data = getTemplate(jsonArray);
        String exec_narrative = executionNarrative(template_data);
        System.out.print("\nExecution Narrative: \n"+exec_narrative);
        String dqnarrative = dataQueryNarrative((String) template_data.get("DataQuery"));
        System.out.println("\n\nData Query Narrative: \n"+dqnarrative);
        //We want to store information regarding all the executions in a hashmap
        Map<String, String> executions = new HashMap<>();
        String hypothesis_narrative = evolvingHypothesisNarrative(surl,prev_url);
        System.out.println("\n\nEvolving Hypothesis Narrative: \n"+hypothesis_narrative);
    }

}
