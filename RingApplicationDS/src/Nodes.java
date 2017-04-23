import java.awt.Button;
import java.awt.Event;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextArea;
import java.awt.TextField;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.sql.Timestamp;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;

/**
 * @author Mahesh Manohar
 * @id 1001396134
 * Nodes implement the client(node) side programming in the Ring Election system where each client will act as a Node in the network
 */
class Nodes extends Frame implements Runnable
{
	private static final long serialVersionUID = 1L;			
	Socket client_Socket;    									//client socket
	TextArea displayText;										//Displaying the messaging being passed in the network
	Label coordinatorLabel;										//Shows the current coordinator of the Ring network
	Button buttonElec,buttonClose;								//Manual election button, Close button that crashes the process
	String node_num;											//Node number that is active (this.thread)
	Thread thread=null;											//client thread
	DataOutputStream dataOutputStream;							//output stream
	DataInputStream dataInputStream;							//input stream
	int coordinator =0;											// Elected Coordinator value
	boolean isTimerSet=false;									//Whether the timer has been started to check if it receives the tokens in correct intervals
	long tokenReceivedTime = 0;									//The time when the token was last received to determine whether the token is lost or not

	/**Open a socket on port 8080. Open the input and the output streams.
	 * Also initializes all the GUI variables.
	 * @param 
	 * @throws Exception
	 */
	Nodes() throws Exception
	{
		super();										
		client_Socket=new Socket("127.0.0.1",8080);										//Open a socket on port 8080. 
		dataInputStream=new DataInputStream(client_Socket.getInputStream()); 			//open input stream
		dataOutputStream=new DataOutputStream(client_Socket.getOutputStream());        	//open output stream
		dataOutputStream.writeUTF("new");												//sending a message for launching new Node
		String messageServer = dataInputStream.readUTF();								//receives the response from server
		node_num=messageServer;															//assigns node number to this node
		coordinatorLabel = new Label();													//creates label object for GUI
		displayText=new TextArea();														//creates text area object for GUI	
		buttonElec=new Button("Election");												//create Election button for GUI
		buttonClose=new Button("Close");												//create Close button for GUI
		thread=new Thread(this);														//create thread for this node
		thread.start();																	//Starts the current client thread

	}

	/**Launches the GUI with initial value if any.
	 * @param 
	 * @throws 
	 */
	@SuppressWarnings("deprecation")
	void setup()
	{
		setTitle("Process Number : "+node_num);											//sets the window(GUI) title to give process number
		setSize(500,300);																//sets the size of the grid
		setLayout(new GridLayout(3,1));													//sets the grid layout
		add(displayText);																//adds text area to display all the messages

		Panel p=new Panel();															//creates a panel object
		p.add(buttonElec);																//adds Election button to panel
		p.add(buttonClose);																//adds Close button to panel
		add(p);																			//adds panel to layout

		coordinatorLabel.setText("Coordinator : ");										//setting the value of label to display coordinator		
		add(coordinatorLabel);															//adds label to layout	

		show();        																	//shows the window
	}

	/* This method on button click from GUI and does the respective operation
	 * 1. Manual Election 2. Crashes the process
	 * @see java.awt.Component#action(java.awt.Event, java.lang.Object)
	 * performs action on a button click
	 */
	@SuppressWarnings("deprecation")
	public boolean action(Event e,Object o)
	{
		//when Election button is clicked
		if(e.arg.equals("Election")){											

			try {
				displayText.setText("Election Token ("+node_num+") : "+node_num);				//shows the election token of this node
				dataOutputStream.writeUTF(node_num+"$"+"Election"+"$"+String.valueOf(node_num)+"$"+" "+"$"+" ");		//sends election token to next node
				dataOutputStream.flush();														//flushes the output stream 
			} catch (Exception e1) {
				e1.printStackTrace();
			}    
		}
		//when Close button is pressed
		else if(e.arg.equals("Close"))															
		{
			try
			{
				dataOutputStream.writeUTF(node_num+ "$"+ "Close"+"$"+" "+"$"+" "+"$"+" ");		//sends stream to close the socket connection to its thread
				System.exit(1);																	//closes the node
			}
			catch(Exception ex)
			{
				ex.printStackTrace();
			}
		}
		return super.action(e,o);
	}

	/**
	 * Starting the client(node) function by initializing all the needed values
	 * No command line arguments needed
	 * @param  
	 * @throws Exception
	 */
	public static void main(String args[]) throws Exception
	{
		Nodes Client1=new Nodes();    //calls the nodes constructor creating the node thread
		Client1.setup();  			  //shows the GUI	
	}    

	/* 
	 * @see java.lang.Runnable#run()
	 * Thread run method which receives all the messaging from server and displays it on the text area of the client.
	 * Also does all the manipulations of election coordinator, starts elections when necessary and also passes token around the network
	 */
	public void run()
	{        
		while(true)
		{
			try
			{	
				String msg=dataInputStream.readUTF();											//reads message from  server
				StringTokenizer stringTokenizer=new StringTokenizer(msg,"$");					//string tokenizes the message
				String sender_node=stringTokenizer.nextToken();									//node from which the msg is received
				String type=stringTokenizer.nextToken();										//type of the message(Election,Re-elect,Coordinator or token)
				String elec_list=stringTokenizer.nextToken();									//token passed from previous node with string as election token
				String cor_node_elector= stringTokenizer.nextToken();							//node that elected the coordinator
				String cor_node= stringTokenizer.nextToken();									//coordinator node 
				String elect_list_forward=elec_list+"-"+String.valueOf(node_num);				//the election list the is forwarded to the next node
				Timer myTimer = new Timer();													//creates timer object
				TimerTask myTask = new TimerTask(){												//creates task for the timer
					public void run(){
						long timeDiff = System.currentTimeMillis() - tokenReceivedTime;			//time difference between last received token and now
						if(timeDiff > 50000 && isTimerSet && coordinator!=0){					// if time > 50 seconds start re-election
							try {
								displayText.setText("\nConducting Re-election!");				//shows re-election msg
								displayText.append("Election Token ("+node_num+") : "+node_num);	//shows the election token of this node
								dataOutputStream.writeUTF(node_num+"$"+"Election"+"$"+String.valueOf(node_num)+"$"+" "+"$"+" ");  //sends election token to next node
								isTimerSet=false;												//sets timer to false so that is stopped until token passing start again
								coordinator=0;													//sets coordinator value to 0 as re-election is initiated
								coordinatorLabel.setText("Coordinator : ");						//label updated the coordinator value
							} catch (Exception e) {
								e.printStackTrace();
							}		
						}
					}
				};
				if(type.contains("Election")){
					displayText.append("\n Incoming Token from "+sender_node+" : " +elec_list); 		//election token from previous node
					//coordinator=0;																	
					thread.sleep(3000);																	//stops thread execution for some time for visibility 				
					if(elec_list.contains(String.valueOf(node_num))){									//if the token has gone one complete round the network
						String[] node_tokens = elec_list.split("-");									
						int max_node = Integer.parseInt(node_tokens[0]);								//find the coordinator
						for (int i = 1; i < node_tokens.length; i++) {									//by finding the max index node		
							if (Integer.parseInt(node_tokens[i]) > max_node) {
								max_node =Integer.parseInt(node_tokens[i]);
							}	
						}
						coordinator=max_node;															//assigns the max value to coordinator 
						elect_list_forward=String.valueOf(node_num);									//token to forward
						coordinatorLabel.setText("Coordinator : "+coordinator);							//displays coordinator on label	
						displayText.append("\n The new Coordinator is :"+max_node); 					//displays coordinator on text area
						//sends new coordinator elected token to all the nodes
						dataOutputStream.writeUTF(node_num+"$"+"Coordinator"+"$"+String.valueOf(elect_list_forward)+"$"+String.valueOf(node_num)+"$"+String.valueOf(coordinator));  	
					}else if(!elec_list.contains(String.valueOf(node_num))){
						displayText.append("\n Election Token("+node_num+") : "+elect_list_forward);     //shows the election token of this node
						dataOutputStream.writeUTF(node_num+"$"+"Election"+"$"+String.valueOf(elect_list_forward)+"$"+" "+"$"+" ");	//sends the eletion token to next node
					}
				}else if(type.contains("Coordinator")){
					coordinator=Integer.parseInt(cor_node);							//assigns the coordinator
					thread.sleep(3000);	
					if(elec_list.contains(String.valueOf(node_num))){				//if the coordinator elected msgs goes one round the network				
						displayText.append("\nConnection Check token");				//display some token to pass around
						elect_list_forward=String.valueOf(node_num);				
						dataOutputStream.writeUTF(node_num+"$"+"Token"+"$"+String.valueOf(elect_list_forward)+"$"+" "+"$"+" ");  //forward the token to next node
						tokenReceivedTime=System.currentTimeMillis();				//time when the token is received or sent
						isTimerSet=true;											//timer is set
						myTimer.schedule(myTask, 1000, 30000);						//start the timer task to check is the token gets lost when the coordinaor is crashed
					}else{
						displayText.append("\nCoordinator elected by "+cor_node_elector+"!New Coordinator : " +cor_node);  //coordinator elected token to be passed
						coordinatorLabel.setText("Coordinator : "+coordinator);		//sets label to the coordinator
						dataOutputStream.writeUTF(node_num+"$"+"Coordinator"+"$"+elec_list+"$"+cor_node_elector+"$"+cor_node);	//send elected coordinator msg to next node
					}
				}else if(type.contains("Token")){
					thread.sleep(3000);
					if(isTimerSet){													//if timer set 
						tokenReceivedTime=System.currentTimeMillis();				//time when the token is received
					}else{
						tokenReceivedTime=System.currentTimeMillis();				//time when the token is received
						isTimerSet=true;											//set the timer task 		
						myTimer.schedule(myTask, 1000, 30000);						//if timer is not set
					}
					
					if(coordinator==0){												//if coordinator crashed stop sending the connection check token

					}else if(elec_list.contains(String.valueOf(node_num))){			
						displayText.append("\nConnection Check token");				//display connection check token	
						elect_list_forward=String.valueOf(node_num);				
						dataOutputStream.writeUTF(node_num+"$"+"Token"+"$"+String.valueOf(elect_list_forward)+"$"+" "+"$"+" ");  //send the connection check token to next node
					}else{
						displayText.append("\nConnection Check token");				//display connection check token	
						dataOutputStream.writeUTF(node_num+"$"+"Token"+"$"+String.valueOf(elect_list_forward)+"$"+" "+"$"+" ");	 //send the connection check token to next node
					}
				}else if(type.contains("ReElect")){						//when new node enters start re-election
					coordinator=0;										//makes the coordinator 0 as re-election is happening
					displayText.setText("\nConducting Re-election!");	//display re-election message
					displayText.append("Election Token("+node_num+") : "+node_num);			//display election token of this node
					thread.sleep(3000);
					dataOutputStream.writeUTF(node_num+"$"+"Election"+"$"+String.valueOf(node_num)+"$"+" "+"$"+" ");		 //sends election token to next node
				}
			}
			catch(Exception ex)
			{
				ex.printStackTrace();
			}
		}
	}
}
