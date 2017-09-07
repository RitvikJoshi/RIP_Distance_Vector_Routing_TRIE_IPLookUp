/*
 * Project2 - Distance Vector Routing
 * This program implements RIP protocol to update routing tables of the routers connected in the network.
 * To run the program:
 * java RIP <router ip_address> <neigbhor ip_address> <neigbhor cost>  ......
 * Author: Ritvik Joshi
 *  
 */


import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.*;
import java.util.regex.Pattern;



/*
 * Main RIP class
 * Provides routing table(Hashtable),receving_queue,sending_queue,neigbhor list to other classes 
 */
public class RIP{

	public static DatagramSocket router_server;
	public static String myipaddr;
	public static DatagramSocket client_socket;
	public static int port =3020;
	public static int[] subnet = {255,255,255,0}; 
	public static ObjectInputStream in;
	public static ObjectOutputStream out;
	public static Hashtable<String,path> routing_table= new Hashtable<String,path>();
	public static Queue<String> sending_queue =  new LinkedList<String>();
	public static Queue<String> receving_queue =  new LinkedList<String>();
	public static ArrayList<neigbhors> direct_neigbhor = new ArrayList<neigbhors>();
	
	
	//Returns network prefix
	public String getkey(String dest_ip){
		String []dest = dest_ip.split(Pattern.quote("."));
		String network_prefix="";
		for(int index=0;index<dest.length;index++){
			int num = Integer.parseInt(dest[index]);	
			network_prefix +=(num & subnet[index]);
			if(index+1!=dest.length){
				network_prefix +=".";
			}
		}
		
		return network_prefix; 
		
		
	}
	//To display Routing Table
	public void printhashtable(){
		Iterator iter;
		iter = routing_table.entrySet().iterator();
		
		System.out.println("Dest. Ip Addr\tSubnet\t\tNexthop\t\tCost");
		while(iter.hasNext()){
			String send_msg="";
			Map.Entry item = (Map.Entry) iter.next();
			path temp_path = (path) item.getValue();
			//send_msg+="Routing Version:: "+2+" ";
			send_msg+=temp_path.dest_addr+"\t"+"255.255.255.0"+"\t"+
					temp_path.Next_hop+"\t"+temp_path.cost;
			System.out.println(send_msg);
			}
		
	}
	
	//Remove failed router from the data structures
	public boolean remove_failed_router(String dest_ip){
		boolean flag=false;
		Iterator iter;
		iter = routing_table.entrySet().iterator();
		
		String send_msg="";
		//remove from neibhor list
		synchronized(direct_neigbhor){
			for(neigbhors nbr: direct_neigbhor){
				if(nbr.dest_ip.equals(dest_ip)){
					direct_neigbhor.remove(nbr);
					break;
				}
		
			}
		}
		
		//removing from hash table next hop
		while(iter.hasNext()){
			
			Map.Entry item = (Map.Entry) iter.next();
			path temp_path = (path) item.getValue();
			if (temp_path.Next_hop==dest_ip){
				iter.remove();
				flag=true;
			}
		}
		// remove from key
		String key= getkey(dest_ip);
		if(routing_table.containsKey(key)){
			routing_table.remove(key);
			flag=true;
		}
		//
		
		
		return flag;
	}
	
	//Main function
	public static void main(String args[]){
		RIP rip2 = new RIP();
		
		if(args.length>0 && args.length%2!=0){
		myipaddr = args[0];
		System.out.println("My IP address....."+myipaddr);
		//Initialize routing table
		for(int i=1;i<args.length;i+=2){	
			System.out.println("Adding neigbhors..."+args[i]);
			direct_neigbhor.add(new neigbhors(args[i],Integer.parseInt(args[i+1]),true));
			String key = rip2.getkey(args[i]);
			path temp_path =  new path(args[i],args[i],Integer.parseInt(args[i+1]));
			routing_table.put(key, temp_path);
		}
		//create threads for every other class			
		rip2.printhashtable();	
		System.out.println("RIP activated");
		System.out.println("Creating  sender thread ");
		new Thread(new send_info(sending_queue)).start();
		System.out.println("Sender thread created ");
		System.out.println("Creating  recevier thread ");
		new Thread(new recevie_info(receving_queue)).start();
		System.out.println("recevier thread created ");
		System.out.println("Creating update thread ");
		new Thread(new update_info(routing_table,receving_queue)).start();
		System.out.println("update thread created");
		System.out.println("Creating mointor thread ");
		new Thread(new moniter()).start();
		System.out.println("monitor thread created");
		}
	}
	
}

 
class moniter extends RIP implements Runnable{

	@Override
	public void run() {
		//Monitor if routers are active or not		
		boolean fail_flag=false;
		String failure_message="";
		String dest_ip="";
		while(true){
			synchronized(direct_neigbhor){
				for(neigbhors nbr: direct_neigbhor){
					if(nbr.active){
						nbr.active=false;
					}else{
						System.out.println("Neighbor down.......... "+nbr.dest_ip);
						dest_ip=nbr.dest_ip;
						fail_flag=true;
					}
				}
			}
			if(fail_flag){
				fail_flag=false;
				failure_message="F"+";"+dest_ip;
				remove_failed_router(dest_ip);	
				synchronized(sending_queue){
					sending_queue.add(failure_message);
					try {
						sending_queue.wait();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
			try {
				//sleeping for 10sec
				Thread.sleep(20000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	
	
	
}

//Neighbor class
class neigbhors{
	String dest_ip;
	int cost;
	boolean active;
	
	neigbhors(String dest,int cost,boolean active){
		this.dest_ip=dest;
		this.cost =cost;
		this.active=active;
	}
}
//routing message format
class routing_message{

	String dest_Ip_addr;
	String src_Ip_addr;
	String Subnet_mask;
	String Next_hop;
	int cost;
	
	routing_message(String dst_Ip_addr,String src_Ip_addr,String Subnet_mask,String Nexthop, int cost){
		
		this.dest_Ip_addr = dst_Ip_addr;
		this.src_Ip_addr = src_Ip_addr;
		this.Subnet_mask=Subnet_mask;
		this.Next_hop=Nexthop;
		this.cost=cost;
	}
}

//Path to be stored in Routing table
class path{
	String dest_addr;
	String Next_hop;
	int cost;
	path(String dest_addr,String Nexthop,int cost){
		this.dest_addr=dest_addr;
		this.Next_hop=Nexthop;	
		this.cost=cost;
	}
}

//Send info
class send_info  extends RIP implements Runnable {

	public Queue<String> queue;
	
	
	send_info(Queue<String> send_queue){
		this.queue = send_queue;
	}
	
	public void send_message(){
		Iterator iter;
		iter = routing_table.entrySet().iterator();
		
		String send_msg="";
		while(iter.hasNext()){
			
			Map.Entry item = (Map.Entry) iter.next();
			path temp_path = (path) item.getValue();
			send_msg+=2+";";
			send_msg+=temp_path.dest_addr+";"+myipaddr+";"+"255.255.255.0"+";"+temp_path.Next_hop+";"+temp_path.cost+";";
			}
			//	System.out.println("message Waiting for sending queue");
	
			//System.out.println("message acquired for sending queue");
			sending_queue.add(send_msg);
	}
	
	
	@Override
	public void run() {
		while(true){
			send_message();
			//System.out.println("Sending thred running");
			//System.out.println("Sending queue:: "+sending_queue.size());
			if(sending_queue.size()>0){
				//System.out.println("sender Waiting for queue");
				//System.out.println("sender acquired for queue");
				String message = sending_queue.remove();
				synchronized(sending_queue){	
					/*if(message.charAt(0)=='F'){
						try {
							sending_queue.wait();
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}*/
					//System.out.println("message"+message);
					
					synchronized(direct_neigbhor){
						if(direct_neigbhor.size()==0){
							System.out.println("No Neigbhors To Send");
							
						}
						for(neigbhors nbr: direct_neigbhor){
							try{
							router_server = new DatagramSocket();
					
							InetAddress ipAddr = InetAddress.getByName(nbr.dest_ip);
							ByteArrayOutputStream Byte_Array = new  ByteArrayOutputStream();
							out = new ObjectOutputStream(Byte_Array);
					        out.writeObject(message);
							
					        byte[] buffer = Byte_Array.toByteArray();
							DatagramPacket packet= new DatagramPacket(buffer,buffer.length,ipAddr,port);
							router_server.send(packet);
							System.out.println("Sending data to "+nbr.dest_ip);
							//System.out.println("message::"+message);
							out.flush();
										
							}catch(Exception e){
								//router_server.close();
								e.printStackTrace();
							}
						}
					}
					printhashtable();
					try {
						sending_queue.wait(3000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
						
					
				}
				
			}
			
			
			
		}
		
	}	
}	
	
	

//update info
class update_info extends RIP implements Runnable{
	public Hashtable<String,path> routing_table;
	public Queue<String> receving_queue;
	update_info(Hashtable<String,path> table,Queue<String> receving_queue){
		this.routing_table = table;
		this.receving_queue = receving_queue;
	}
	
	public void poison_river(routing_message msg){
		
		synchronized(direct_neigbhor){
			for(neigbhors nbr: direct_neigbhor){
				String dkey =  getkey(msg.dest_Ip_addr);
				path curr_path= (path) routing_table.get(dkey);
				
				if(nbr.dest_ip==msg.dest_Ip_addr){
					String key =  getkey(msg.src_Ip_addr);
					path msg_router_path;
					if(routing_table.containsKey(key)){
						//System.out.println("Key present");
						msg_router_path= (path) routing_table.get(key);
					}
					else{
						//System.out.println("Key not present");
						msg_router_path = new path(msg.src_Ip_addr,msg.src_Ip_addr,9999999);
					}
					if((msg_router_path.cost+msg.cost) < nbr.cost){
						System.out.println("Poison River Update ...........");
						curr_path.dest_addr= msg.dest_Ip_addr;
						curr_path.Next_hop = msg.src_Ip_addr;
						curr_path.cost = msg_router_path.cost+msg.cost;  
						//System.out.println("key"+key+"cost"+curr_path.cost+" "+routing_table);
						routing_table.put(key, curr_path);
						System.out.println("Updated");
						//flag= true;
					}
					else{
						path temp_path= new path(nbr.dest_ip,nbr.dest_ip,nbr.cost);
						routing_table.put(dkey, temp_path);
					}
				}
			}	
		}
		
		
	}
	
	
	
	public boolean update_hashtable(routing_message rm[]){
		boolean flag=false;
		for (routing_message msg: rm){
			if (msg !=null && !msg.dest_Ip_addr.equals(myipaddr) && !msg.Next_hop.equals(myipaddr)){
				//System.out.println("Inside rm");
				//System.out.println("msg......"+msg.dest_Ip_addr);
				
				String key =  getkey(msg.dest_Ip_addr);
				String msg_key =  getkey(msg.src_Ip_addr);
				path msg_router_path;
				//System.out.println("key........"+key);
				if(routing_table.containsKey(key)){
					path curr_path = (path) routing_table.get(key);
					if(curr_path.Next_hop == msg.src_Ip_addr){
						poison_river(msg);
						continue;
					}
					
					
					//System.out.println("message key::"+msg_key);
					
					if(routing_table.containsKey(msg_key)){
						//System.out.println("Key present");
						msg_router_path= (path) routing_table.get(msg_key);
					}
					else{
						//System.out.println("Key not present");
						
						msg_router_path = new path(msg.src_Ip_addr,msg.src_Ip_addr,9999999);
					}
					if((msg_router_path.cost+msg.cost) < curr_path.cost){
						System.out.println("Updating Hashtable...........");
						curr_path.dest_addr= msg.dest_Ip_addr;
						curr_path.Next_hop = msg.src_Ip_addr;
						curr_path.cost = msg_router_path.cost+msg.cost;  
						//System.out.println("key"+key+"cost"+curr_path.cost+" "+routing_table);
						routing_table.put(key, curr_path);
						System.out.println("Updated");
						flag= true;
					}
				}
				else{
					System.out.println("Adding new entry to Hashtable...........");
					if(routing_table.containsKey(msg_key)){
						//System.out.println("Key present");
						msg_router_path= (path) routing_table.get(msg_key);
					}
					else{
						msg_router_path = new path(msg.src_Ip_addr,msg.src_Ip_addr,9999999);
					}
					path curr_path = new path(msg.dest_Ip_addr,msg.src_Ip_addr,msg_router_path.cost+msg.cost);
					
					routing_table.put(key, curr_path);
					flag=true;
					
				}
			}
		}
		
		return flag;
	}
	
	
	@Override
	public void run() {
		String message;
		while(true){
			//System.out.println("update thread running");
			//System.out.println("Receving queue::"+receving_queue.size());
			synchronized(receving_queue){
				String[]msg;
				if(receving_queue.size()>0){
					
					//System.out.println("Update Waiting for receving queue");
					
					//System.out.println("update acquired receing queue");
					message = receving_queue.remove();
					if (message.charAt(0)!='F'){
						msg = message.split(";");
					
						routing_message rm[] =  new routing_message[10];
						int counter=0;
						for(int i=0;i<msg.length;i+=6){
						//System.out.println(msg[i+1]+msg[i+2]+msg[i+3]+msg[i+4]+msg[i+5]);
							rm[counter] = new routing_message(msg[i+1],msg[i+2],msg[i+3],msg[i+4],Integer.parseInt(msg[i+5]));
							counter++;
						}
					
						boolean update_flag=update_hashtable(rm);
						if (update_flag){
							printhashtable();
							System.out.println("Initiating Trigger update");
							synchronized(sending_queue){
								sending_queue.notifyAll();
							}
						}
					}
					else{
						String failure_message[] = message.split(";");
						boolean remove_flag=remove_failed_router(failure_message[1]);
						if(remove_flag){
							synchronized(sending_queue){
								sending_queue.add(message);
								sending_queue.notifyAll();
							}
						}
					}
				}/*else{
						try {
							receving_queue.wait();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}	*/
			}
		}
				
	}
			
}


class recevie_info extends RIP implements Runnable{
	public Queue<String> queue;
	recevie_info(Queue<String> receving_queue){
		this.queue = receving_queue;
		
	}
	
	@Override
	public void run() {
		try{
			client_socket = new DatagramSocket(port);
			byte buff[];
			byte buffer[] = new byte[1024];
			String message;
			
			while(true){
				//System.out.println("Receving thred running");
				message=null;
				DatagramPacket packet = new DatagramPacket(buffer, buffer.length );
				client_socket.receive(packet);
				//System.out.println("Getting input");
				buff = packet.getData();
				ByteArrayInputStream array_input_stream = new ByteArrayInputStream(buff);
				in = new ObjectInputStream(array_input_stream);	
				message= (String) in.readObject();
				String[] msg = message.split(";");
				System.out.println("recevied Input");
				if(message!=null){
					//System.out.println("Recevier Waiting for receving queue");
					synchronized (receving_queue){
						//System.out.println("Recevier acquired receving queue");
						synchronized(direct_neigbhor){
							for(neigbhors nbr: direct_neigbhor){
								if(nbr.dest_ip.equals(msg[1])){
									nbr.active=true;
									System.out.println("neigbhors.... "+nbr.dest_ip+" is active");
									break;
								}
							}
						}
										
						System.out.println("Adding Input to queue");
						queue.add(message);
						receving_queue.notifyAll();
						
						}
				}
			}
		}catch(Exception e){
				//client_socket.close();
				e.printStackTrace();
		}
		
	}
	
}
