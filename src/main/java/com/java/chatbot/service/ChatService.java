package com.java.chatbot.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Properties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.java.chatbot.model.Chat;
import com.java.chatbot.model.Nodes;
import com.java.chatbot.model.Relations;
import com.java.chatbot.repository.NodesRepossitory;
import com.java.chatbot.repository.RelationsRepossitory;
import com.java.chatbot.repository.VocabularyRepossitory;
import com.java.chatbot.util.Constants;
import com.java.chatbot.util.ExcelUtils;

/**
 * @author ${Mahesh M Kumar}
 * @date Jun 18, 2018
 */
@Service
public class ChatService {

	@Value("${chatbot.name}")
	private String chatBotName;
	/*
	 * @Value("${chatbot.welcome.message}") private String welcomeTemplate;
	 * 
	 * @Value("${chatbot.continuation.message}") private String contMessageTemplate;
	 */

	@Autowired
	private SimpMessagingTemplate template;

	@Autowired
	private RelationsRepossitory relationsRepossitory;

	@Autowired
	private NodesRepossitory nodesRepossitory;

	@Autowired
	private VocabularyRepossitory vocabularyRepossitory;

	private static final String NAME_PLACEHOLDER = "<<name>>";
	private static final String RELATIONS_PLACEHOLDER = "<<relations>>";
	private static final String NODE_PLACEHOLDER = "<<node>>";
	//private static HashMap<String, String> map = HashMap<String, String>();
	HashMap<String,String> hm=new HashMap<String,String>();    

	public synchronized String getchatId() {
		return String.valueOf(new Date().getTime());
	}

	public void joinUser(Chat chat) throws Exception {
		ArrayList<Relations> relationsList = null;
		try {
			relationsList = relationsRepossitory.findByParentNodeId(0);
			chat.setContent(buildWelcomeMessgae(chat, relationsList));
		} catch (NullPointerException e) {
			chat.setContent(
					"Looks like Admin is not ready for any suggessions!! Please enter your query, our team will get back to you. Thanks.");
		}
		chat.setType(Chat.MessageType.CHAT);
		chat.setSender(chatBotName);
		//System.out.println("Chat UserName: "+chatBotName);
		template.convertAndSend("/topic/private/" + chat.getChatId(), chat);
	}

	public void sendMessage(Chat chat) {
		System.out.println("Req msg1: "+chat.getContent());
		
			FillMap(chat,"","");
	
		if (chat.getContent() == null || chat.getContent().trim().length() == 0) {
			chat.setContent("looking like you are not entering valid question, Please type your question my friend.");
		} else {
			if (chat.isEndCoversation()) {
				if(chat.isEndChat()){
					//TODO: Trigger Build...USERNAME: chat.getPrevContent(), PASSWORD: chat.getContent()
					System.out.println("USERNAME: "+chat.getPrevContent());
					String Username = chat.getPrevContent();
					//FillMap(chat,Username);
					System.out.println("PASSWORD: "+chat.getContent());
					String Password = chat.getContent();
					FillMap(chat,Username,Password);
					try {
						sendGet();
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
					chat.setContent(vocabularyRepossitory.findById(5).getMessage().replaceAll(NAME_PLACEHOLDER, chat.getSender()));
				}else{
					//Write to prop file
					System.out.println("Req msg: "+chat.getContent());
					
					Nodes node = nodesRepossitory.findByNode(chat.getPrevContent());
					chat.setPrevContent(chat.getContent());
					ArrayList<Relations> relationsList = relationsRepossitory.findByParentNodeId(node.getId());
					if (!relationsList.isEmpty()) {
						chat.setContent(buildContinuationMessgae(chat, relationsList, node.getAllowCustom()));
					}
					chat.setEndChat(true);
				}
			} else if (Constants.ADD_NEW.equals(chat.getContent())) {
				chat.setContent(
						"Please type the name of new property which you wish to add");
				ArrayList<String> keywords = new ArrayList<>();
				keywords.add("CUSTOM");
				chat.setKeywords(keywords);
			} else if (chat.getAllowCustom()==1) {
                saveRecord(chat);
            } else {
				chat.setPrevContent(chat.getContent());
				Nodes node = nodesRepossitory.findByNode(chat.getContent());
				if (node == null) {
					chat.setContent(
							"Looks like you are asking which is out of my context. Please select any product which i mentioned above. Thanks. ");
				} else {
					ArrayList<Relations> relationsList = relationsRepossitory.findByParentNodeId(node.getId());
					if (!relationsList.isEmpty()) {
						chat.setContent(buildContinuationMessgae(chat, relationsList, node.getAllowCustom()));
					}else{
                        Relations relation = relationsRepossitory.findByChildNodeId(node.getId()).get(0);
                        if(relation.getHadNextRelations() == 2){
                            ArrayList<String> keywords = new ArrayList<>();
                            keywords.add(Constants.ADD_NEW);
                            chat.setKeywords(keywords);
                            chat.setAllowCustom(node.getAllowCustom());
                        }
                    }
				}
			}
		}
		chat.setSender(chatBotName);
		template.convertAndSend("/topic/private/" + chat.getChatId(), chat);
	}

    private void saveRecord(Chat chat) {
	    if(nodesRepossitory.findByNode(chat.getContent()) == null){
            nodesRepossitory.save(new Nodes(chat.getContent(), 1));
            Nodes nodetoInsert = nodesRepossitory.findByNode(chat.getContent());
            Nodes previousNode = nodesRepossitory.findByNode(chat.getPrevContent());
            Relations rel = relationsRepossitory.findByParentNodeId(previousNode.getId()).get(0);

            //Attaching new node to parent node
            ArrayList<Relations> relations1 = relationsRepossitory.findByChildNodeId(rel.getChildNodeId());

            for(Relations relations: relations1){
                Relations newRelations = new Relations();
                newRelations.setParentNodeId(relations.getParentNodeId());
                newRelations.setChildNodeId(nodetoInsert.getId());
                newRelations.setVocabularyId(relations.getVocabularyId());
                newRelations.setHadNextRelations(relations.getHadNextRelations());
                relationsRepossitory.save(newRelations);
            }

            //Attaching child nodes to new node

            ArrayList<Relations> prevNodeRelations = relationsRepossitory.findByParentNodeId(rel.getChildNodeId());
            for(Relations relations:prevNodeRelations){
                if(relations.getChildNodeId() != nodetoInsert.getId()){
                    Relations newRelations = new Relations();
                    newRelations.setParentNodeId(nodetoInsert.getId());
                    newRelations.setChildNodeId(relations.getChildNodeId());
                    newRelations.setVocabularyId(relations.getVocabularyId());
                    newRelations.setHadNextRelations(relations.getHadNextRelations());
                    relationsRepossitory.save(newRelations);
                }
            }
            chat.setPrevContent(chat.getPrevContent ());
            ArrayList<Relations> relationsList = relationsRepossitory.findByParentNodeId(previousNode.getId());
            if (!relationsList.isEmpty()) {
                chat.setContent(buildContinuationMessgae(chat, relationsList, 1));
            }
        }else{
            chat.setContent(
                    "Looks like you are adding item is already present in my database. Thanks. ");
        }
    }

    public String buildWelcomeMessgae(Chat chat, ArrayList<Relations> relationsList)  {
    	System.out.println("Chat User: "+chat.getSender());
			writeDataPropFile("to", chat.getSender()+"@oracle.com");
            String welcomeMessage = vocabularyRepossitory.findById(relationsList.get(0).getVocabularyId()).getMessage();
		welcomeMessage = welcomeMessage.replace(NAME_PLACEHOLDER, chat.getSender());
		welcomeMessage = welcomeMessage.replace(RELATIONS_PLACEHOLDER, formatRelations(chat, relationsList, 0));
		return welcomeMessage;
	}

	public String buildContinuationMessgae(Chat chat, ArrayList<Relations> relationsList, int allowCustom) {
		String contMessage = vocabularyRepossitory.findById(relationsList.get(0).getVocabularyId()).getMessage();
		contMessage = contMessage.replace(RELATIONS_PLACEHOLDER, formatRelations(chat, relationsList, allowCustom));
		contMessage = contMessage.replaceAll(NAME_PLACEHOLDER, chat.getSender());
		contMessage = contMessage.replace(NODE_PLACEHOLDER, chat.getContent());
		return contMessage;
	}

	public String formatRelations(Chat chat, ArrayList<Relations> relationsList, int allowCustoms) {
		String nodesNames = "";
		ArrayList<String> keywords = new ArrayList<String>();
		for (Relations relations : relationsList) {
		    String nodeName = nodesRepossitory.findById(relations.getChildNodeId()).getNode();
			nodesNames += nodeName  + ",";
			keywords.add(nodeName);
			if (relations.getHadNextRelations() == 1) {
				chat.setPrevContent(nodeName);
				chat.setEndCoversation(true);
				chat.setDisableButton(true);
			}else if (relations.getHadNextRelations() == 2){
				chat.setDisableButton(true);
			}else{
				chat.setEndCoversation(false);
				chat.setDisableButton(false);
			}
		}
		if(allowCustoms == 1){
			//chat.setAllowCustom(1);
			keywords.add(Constants.ADD_NEW);
		}
		chat.setKeywords(keywords);
		return nodesNames.substring(0, (nodesNames.length() - 1));
	}
	
	@Value("${Excel.path}")
	private  String excelFilePath;
	private void FillMap(Chat chat, String Username, String Password) {
		//String Env = "";
		System.out.println("FillMap: "+chat.getContent());
		try {
		switch (chat.getContent()) {
		case "Bat":
			writeDataPropFile("SUITE", chat.getContent());
			break;
		case "Sanity":
			writeDataPropFile("SUITE", chat.getContent());
			break;
		case "Cloud":
			writeDataPropFile("Prod", chat.getContent());
			break;
		case "V5":
			writeDataPropFile("Prod", chat.getContent());
			break;
		case "1.2":
			writeDataPropFile("Prod", chat.getContent());
			break;
		case "VAB":
			writeDataPropFile("Env", chat.getContent());
			ExcelUtils.setDataByRow(excelFilePath,"RunConfig", "Set", "VALUE", chat.getContent());
			hm.put("Env", chat.getContent());
			System.out.println("Environment: "+hm.get("Env"));
			break;
		case "LPAD":
			writeDataPropFile("Env", chat.getContent());
			ExcelUtils.setDataByRow(excelFilePath,"RunConfig", "Set", "VALUE", chat.getContent());
			hm.put("Env", chat.getContent());
			System.out.println("Environment: "+hm.get("Env"));
			break;
		case "SK003":
			System.out.println("Env in prop: "+hm.get("Env"));
			if(hm.get("Env").contains("VAB"))
			{
				ExcelUtils.setDataByRow(excelFilePath,"Configuration","Property","VAB",chat.getContent());
			}
			else if(hm.get("Env").contains("LP"))
			{
				ExcelUtils.setDataByRow(excelFilePath,"Configuration", "Property", "LPAD", chat.getContent());
			}
			writeDataPropFile("Property", chat.getContent());
			break;
		case "AUTOPROP":
			if(hm.get("Env").contains("VAB"))
			{
				ExcelUtils.setDataByRow(excelFilePath,"Configuration", "Property", "VAB", chat.getContent());
			}
			else if(hm.get("Env").contains("LP"))
			{
				ExcelUtils.setDataByRow(excelFilePath,"Configuration", "Property", "LPAD", chat.getContent());
			}
			writeDataPropFile("Property", chat.getContent());
			break;
		      
		default:
			break;
		}
		
		if (Username != null && !Username.isEmpty()) {
			System.out.println("User name in FillMap: "+Username);
			
			if(hm.get("Env").equalsIgnoreCase("VAB"))
			{
				ExcelUtils.setDataByRow(excelFilePath,"Configuration", "UserName", "VAB", Username);
			}
			else if(hm.get("Env").equalsIgnoreCase("LP"))
			{
				ExcelUtils.setDataByRow(excelFilePath,"Configuration", "UserName", "VAB", Username);
			}
			
			}
		
         if (Password != null && !Password.isEmpty()) {
        	 System.out.println("Password in FillMap: "+Password);
			if(hm.get("Env").equalsIgnoreCase("VAB"))
			{
				ExcelUtils.setDataByRow(excelFilePath,"Configuration", "Password", "VAB", Password);
			}
			else if(hm.get("Env").equalsIgnoreCase("LP"))
			{
				ExcelUtils.setDataByRow(excelFilePath,"Configuration", "Password", "VAB", Password);
			}
			
			}
		
		
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		/*switch (chat.getPrevContent()) {
		case "Bat":
			writeDataPropFile("SUITE", chat.getContent());
			break;
		}*/
	}
	
	@Value("${Envprop.file}")
	private  String strGlobalDataSheetPath;
	@Value("${Configprop.file}")
	private  String configPath;
	
	private void writeDataPropFile(String key, String value) 
	{
		System.out.println("In writeDataPropFile: "+key +" "+value);
		System.out.println("PATH: "+strGlobalDataSheetPath);
		String path =strGlobalDataSheetPath;
		//String path = "D:\\GitRepo\\OPERACloud\\automation\\catalog\\testdata\\metadata\\environment.properties";
		try{
			if(key.equals("to"))
			{
				path=configPath;
			}

			File file = new File(path);
			Properties prop = new Properties();

			FileInputStream fIn = new FileInputStream(file);
			prop.load(fIn);
			fIn.close(); 

			FileOutputStream fOut = new FileOutputStream(file);
			if(value == "")
				prop.remove(key);         
			else
				prop.setProperty(key,value);
			prop.store(fOut,"Properties");
			fOut.close();       
		}
		catch(Exception e)
		{
			System.out.println("Exception occured :: "+ e.getMessage());
		}
	}
	
	@Value("${jenkins.url}")
	private String jenkinsUrl;
	private void sendGet() throws Exception {
		
		//String url = "http://localhost:8080/job/BAT/build?token=7d3eed482526f1398efe03528a58d4c8";
		
		URL obj = new URL(jenkinsUrl);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();

		// optional default is GET
		con.setRequestMethod("GET");

		//add request header
		//con.setRequestProperty("User-Agent", USER_AGENT);

		int responseCode = con.getResponseCode();
		System.out.println("\nSending 'GET' request to URL : " + jenkinsUrl);
		System.out.println("Response Code : " + responseCode);

		BufferedReader in = new BufferedReader(
		        new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();

		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();

		//print result
		System.out.println(response.toString());

	}
	
	
	
	public static void envVariables(){
		
		/*System.out.println("Req msg: "+chat.getContent());
		switch (chat.getContent()) {
		case "BAT":
			writeDataPropFile("SUITE", chat.getContent());
			break;
		case "Sanity":
			writeDataPropFile("SUITE", chat.getContent());
			break;
		case "Automation":
			writeDataPropFile("Env", chat.getContent());
			break;
		case "VAB":
			writeDataPropFile("Env", chat.getContent());
			break;
		case "PROP1":
			writeDataPropFile("Property", chat.getContent());
			break;
		case "AUTOPROP":
			writeDataPropFile("Property", chat.getContent());
			break;
		}*/
		
	}

}
