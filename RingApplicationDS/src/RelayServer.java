import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;

/**
 * @author Mahesh Manohar
 * @id 1001396134
 * RelayServer class performs the function of the server which needs to be started before running any client and relays the messaging between the nodes
 */

class RelayServer 
{
	public static ArrayList<Socket> socket_client=null; 		// ArrayList of socket_client stores the Socket information of multiple clients(nodes) that are alive
	public static ArrayList<Integer> nodeNums=null;				// ArrayList of Node numbers  
	ServerClient ServerClient =null;							// Server Client is an object reference to the class AcceptClient
	ServerSocket socket_server=null;							// socket_server is instance of  server socket class that is used by multiple clients to connect to a server
	int port;											  	 	// port is a number between 1 and 65535 that binds a particular server socket
	int coordinator =0,prev_coordinator=0;						//coordinator and previous coordinator values 
	boolean isElection= false;									// whether an election had been conducted for setting the initial network setup

	/**RelayServer constructor is invoked by passing the desired port number while instantiating the RelayServer class object. 
	 * Opens a server socket on port 8080
	 * Creates a Socket object from the ServerSocket to listen to and accept connections
	 * @param port
	 * @throws Exception
	 */
	RelayServer(int port) throws Exception
	{
		try{
			System.out.println("Server started on port number :"+port);		// Prints the message on server notifying the start of the server along with port number
			this.port=port;											   		// Sets the port number to the class variable port
			socket_server=new ServerSocket(port);							// Opens a server socket on port 8080
			socket_client=new ArrayList<Socket>();					  		// Instantiates ClientSockets arraylist described above
			nodeNums=new ArrayList<Integer>();						   		// Instantiates Node number arraylist described above

			while(true)
			{    
				Socket cSocket=socket_server.accept();        				 //Creates a Socket object from the ServerSocket to listen to and accept connections
				ServerClient=new ServerClient(cSocket);					     // Passes the client socket object to the AcceptClient class that performs input and output data stream operations from and to client
			}
		}
		catch(Exception ex){
			ex.printStackTrace();
		}
	}

	/**passes the port number 8080 to the parameterized constructor
	 * No command line arguments needed
	 * @param args 
	 * @throws Exception
	 */
	@SuppressWarnings("unused")
	public static void main(String args[]) throws Exception
	{
		RelayServer instantMessagingSystem_Server=new RelayServer(8080); 		//Passes the port number 8080 to the parameterized constructor
	}

	/**
	 * ServerClient Opens input and output streams and receives and sends messages from and to the clients.
	 * Connects to multiple clients simultaneously using multithreading
	 *
	 */
	class ServerClient extends Thread
	{
		Socket client_Socket;									// The client socket
		DataInputStream dataInputStream;						// The input stream
		DataOutputStream dataOutputStream;						// The output stream

		/**
		 * Accepts client connections and opens a socket for the same
		 * @param client socket client_Socket
		 * @throws Exception
		 */
		ServerClient (Socket cSocket) throws Exception
		{
			client_Socket=cSocket;								
			dataInputStream=new DataInputStream(client_Socket.getInputStream());
			dataOutputStream=new DataOutputStream(client_Socket.getOutputStream());
			int new_node_num=0;															//stores the new node number
			String clientValue=dataInputStream.readUTF();                          		// reads the value from Node for setting up the network of those nodes
			if(clientValue!=null){								                        
				System.out.println("New Node Entry");									// add new node to the network
				if(nodeNums.isEmpty()){
					dataOutputStream.writeUTF("1");
				}else{
					new_node_num= nodeNums.get(nodeNums.size() - 1);					// add new node to the network
					dataOutputStream.writeUTF(String.valueOf(new_node_num+1));			//send the new node number to itself
				}
			}
			nodeNums.add(new_node_num+1);												//add new node number to arraylist
			System.out.println("New node added to the Network : " + (new_node_num+1));	//prints the added node number
			socket_client.add(client_Socket);   										// Adds the current client socket to the list of client sockets
			start();																	// Calls the run() to run multiple clients(threads) concurrently
			if(isElection){																//if an election had been conducted before this node entry
				dataOutputStream.writeUTF(" "+"$"+"ReElect"+"$"+" "+"$"+" "+"$"+" ");	//this client node perform election on entry
			}
		}

		/* (non-Javadoc)
		 * @see java.lang.Thread#run()
		 * ServerClient Opens input and output streams and receives and sends messages from and to the clients.
		 */
		public void run()
		{
			while(true)
			{

				try
				{
					String msgFromNode=new String();								//msg to be relayed
					msgFromNode=dataInputStream.readUTF();							//msg from client node
					StringTokenizer stringTokenizer=new StringTokenizer(msgFromNode,"$");	//tokenize the string based on $
					int nodeNum =Integer.parseInt(stringTokenizer.nextToken());    			//node from which the msg has been received			
					String messageHint=stringTokenizer.nextToken();							//type of msg (mentioned in below if conditions)
					String elec_list=stringTokenizer.nextToken();							//election token list of nodes
					String cor_node_elector=stringTokenizer.nextToken();					//node that elected the coordinator	
					String cor_node=stringTokenizer.nextToken();							//coordinator node
							
					int nodeCount=0;												//index of arraylist of nodes
					int next_node=0;												//the node to which the message has to be sent
					if(messageHint.equals("Election")){								//if election msg
						isElection=true;											//first election has been conducted
						for(nodeCount=0;nodeCount<nodeNums.size();nodeCount++){		//sends the msg to the next node	
							if(nodeNums.get(nodeCount).equals(nodeNum)&&!(nodeCount==nodeNums.size()-1)){
								next_node = nodeCount+1;							//next node
								Socket clientSoc=(Socket)socket_client.get(next_node);                          //Get the socket information  
								DataOutputStream dataOutputStream=new DataOutputStream(clientSoc.getOutputStream());    //Data output
								dataOutputStream.writeUTF(nodeNum+"$"+"Election"+"$"+elec_list+"$"+" "+"$"+" ");			//forwards the election msg to next node
								dataOutputStream.flush();
								break;
							}else if(nodeNums.get(nodeCount).equals(nodeNum) && (nodeCount==(nodeNums.size()-1))){
								next_node=nodeNums.size()-1-nodeCount;								//next node
								Socket clientSoc=(Socket)socket_client.get(next_node);                          //Get the socket information  
								DataOutputStream dataOutputStream=new DataOutputStream(clientSoc.getOutputStream());    //Data output
								dataOutputStream.writeUTF(nodeNum+"$"+"Election"+"$"+elec_list+"$"+" "+"$"+" ");			//response header with logged out message to other client through the server
								dataOutputStream.flush();
								break;
							}
						}

					}
					else if(messageHint.equals("Close"))											//Client requests to close by hitting Close button
					{
						for(nodeCount=0;nodeCount<nodeNums.size();nodeCount++)
						{
							if(nodeNums.get(nodeCount).equals(nodeNum))							//Itreates through list of nodes
							{
								nodeNums.remove(nodeCount);										//removes the node that wants to Close
								socket_client.remove(nodeCount);									//removes the node's socket from list
								System.out.println("Node " + nodeNum +" Disconnected from the network!");			//prints the information of logged out client on server
								if(coordinator==nodeNum){
									coordinator=0;												//make coordinator to 0 if its logs out (only for server reference)
									prev_coordinator=nodeNum;
									break;
								}
								break;
							}
						}
						break;
					}else if(messageHint.equals("Coordinator")){						//coordinator token passing
						coordinator=Integer.parseInt(cor_node);
						for(nodeCount=0;nodeCount<nodeNums.size();nodeCount++){					//sends the coordinator to next node
							if(nodeNums.get(nodeCount).equals(nodeNum)&&!(nodeCount==nodeNums.size()-1))	{
								next_node = nodeCount+1;								//next node
								Socket clientSoc=(Socket)socket_client.get(next_node);                          //Get the socket information  
								DataOutputStream dataOutputStream=new DataOutputStream(clientSoc.getOutputStream());    //Data output
								dataOutputStream.writeUTF(nodeNum+"$"+"Coordinator"+"$"+elec_list+"$"+cor_node_elector+"$"+cor_node);		//send the coordinator elected msg to next node
								break;
							}else if(nodeNums.get(nodeCount).equals(nodeNum) && nodeCount==nodeNums.size()-1){
								next_node=nodeNums.size()-1-nodeCount;					//next node
								Socket clientSoc=(Socket)socket_client.get(next_node);                          //Get the socket information  
								DataOutputStream dataOutputStream=new DataOutputStream(clientSoc.getOutputStream());    //Data output
								dataOutputStream.writeUTF(nodeNum+"$"+"Coordinator"+"$"+elec_list+"$"+cor_node_elector+"$"+cor_node);		//send the coordinator elected msg to next node
								break;
							}
						}
					}else if(messageHint.equals("Token")){					//connection token passing around then network
						for(nodeCount=0;nodeCount<nodeNums.size();nodeCount++){
							if(nodeNums.get(nodeCount).equals(nodeNum)&&!(nodeCount==nodeNums.size()-1))	{
								next_node = nodeCount+1;
								if(coordinator==0 && nodeCount==prev_coordinator-2){			//if no coordinator the ring is broken
									break;
								}
								Socket clientSoc=(Socket)socket_client.get(next_node);                          //Get the socket information  
								DataOutputStream dataOutputStream=new DataOutputStream(clientSoc.getOutputStream());    //Data output
								dataOutputStream.writeUTF(nodeNum+"$"+"Token"+"$"+elec_list+"$"+" "+"$"+String.valueOf(coordinator));			//send the connection check msg to next node
								break;
							}else if(nodeNums.get(nodeCount).equals(nodeNum) && nodeCount==nodeNums.size()-1){
								next_node=nodeNums.size()-1-nodeCount;
								if(coordinator==0 && nodeCount==prev_coordinator-2){          //if no coordinator the ring is broken
									break;
								}
								Socket clientSoc=(Socket)socket_client.get(next_node);                          //Get the socket information  
								DataOutputStream dataOutputStream=new DataOutputStream(clientSoc.getOutputStream());    //Data output
								dataOutputStream.writeUTF(nodeNum+"$"+"Token"+"$"+elec_list+"$"+" "+"$"+String.valueOf(coordinator));			//send the connection check msg to next node
								break;
							}
						}
					}
				}
				catch(Exception ex)
				{
					ex.printStackTrace();
				}
			}        
		}
	}
}