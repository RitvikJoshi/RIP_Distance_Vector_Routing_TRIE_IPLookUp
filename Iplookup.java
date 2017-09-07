/**
 * This program implements IP forwarding techniques using Longest Prefix Matching
 * 1. Naive Approach - Involve 32 key searches in Hash table generated using all possible subnet mask(32)
 * 2. Trie Approach - Creates a binary bit tree of all the network prefix present in the routing table
 * 					  Perform Longest prefix matching between Ip address to be forwarded and Trie tree.
 * 
 * Author: Ritvik Joshi	 
 */


import java.util.*;
import java.util.regex.Pattern;
import java.io.*;

/*
 * Value class
 * Stores value(Next hop, subnet mask) of routing table
 */
class value{
	String nexthop;
	String subnet_mask;
	
	value(String nexthop,String subnet_mask){
		this.nexthop=nexthop;
		this.subnet_mask=subnet_mask;
	}
}

/*
 * Main Class
 * Take Input and test file name as an command line argument
 * Create forwarding hash table for Naive approach
 * Perform bitwise operation on Ip address to get network prefix
 * Prints result into output file
 */
public class Iplookup {
	//Forwarding Hash table(Naive approach)
	public static Hashtable<String,value> forwarding_table= new Hashtable<String,value>(); 
		
	//Convert Ip address into Binary network prefix
	public String get_bin_prefix(String input, int CIDR){
		String buffer[] = input.split("\\.");
		String bin_val="";
		//converting into binary string
		for(int i=0;i<buffer.length;i++){
			bin_val+=String.format("%8s", Integer.toBinaryString(Integer.parseInt(buffer[i]))).replace(' ', '0');
		}
		// Network prefix
		String prefix = bin_val.substring(0,CIDR);
				
		return prefix;
	}
	
	//Converts CIDR into subnet mask
	public String get_subnet_mask(int CIDR){
		int fixed_width = (int)CIDR/8;
		int additional_bit = CIDR%8;
		String subnet_mask ="";
		boolean bit_flag;
		if(additional_bit>0){
			bit_flag=true;
		}else{
			bit_flag=false;
		}
		
		for(int i=0;i<4;i++){
			
			if(i<fixed_width){
				subnet_mask+="255";
			}else{
				if(bit_flag){
					int bit_sum=0;
					for(int j=0;j<additional_bit;j++){
						bit_sum+=Math.pow(2, (7-j));
					}
					subnet_mask+=""+bit_sum;
					bit_flag=false;
				}else{
					subnet_mask+="0";
				}
			}
			if(i+1==4){
				continue;
			}else{
				subnet_mask+=".";
			}
		}
		return subnet_mask;
	}
	
	//Returns all possible subnet mask
	public ArrayList<String> get_all_subnet_mask(){
		ArrayList<String> subnet =  new ArrayList<String>();
		int fixed_width=0;
		int additional_bit=0;
		int bit_sum=0;
		for(int i=0;i<32;i++){
			String subnet_mask="";
			if(additional_bit<7){
				additional_bit++;
			}
			else{
				additional_bit=0;
				fixed_width+=1;
			}
			boolean bit_flag=true;
			for(int j=0;j<4;j++){
				if(j<fixed_width){
					subnet_mask+="255";
				}else{
					if(bit_flag){
						bit_sum=0;
						for(int k=0;k<additional_bit;k++){
							bit_sum+=Math.pow(2, (7-k));
						}
						subnet_mask+=""+bit_sum;
						bit_flag=false;
					}else{
						subnet_mask+="0";
					}
				}
				if(j+1==4){
					continue;
				}else{
					subnet_mask+=".";
				}
			}
			subnet.add(subnet_mask);
		}
		
		return subnet;
		
	}
	
	//Perform bitwise operation on Ip address and subnet mask
	//Return network prefix
	public String getkey(String ip, String subnet_mask){
		String []dest = ip.split(Pattern.quote("."));
		String []subnet = subnet_mask.split(Pattern.quote("."));
		String network_prefix="";
		for(int index=0;index<dest.length;index++){
			int num = Integer.parseInt(dest[index]);
			int subnet_num = Integer.parseInt(subnet[index]);
			network_prefix +=(num & subnet_num);
			if(index+1!=dest.length){
				network_prefix +=".";
			}
		}
		
		return network_prefix; 
		
		
	}
	
	/*
	 * Naive lookup 
	 * Perform IP look up using Naive approach  
	 */
	
	@SuppressWarnings("unused")
	public void naive_lookup(ArrayList<String> Test,ArrayList<String> subnet){
		Iplookup Ip = new Iplookup();
		int def_count=0;
		int match_Count=0;
		boolean first_flag=true;
		ArrayList<String> out =  new ArrayList<String>();
		
		long timeTaken=0;
		//Iteration loop == 100
		for(int k=0;k<100;k++){	
			long startTime = System.currentTimeMillis();
			//Input array
			for(int i=0;i<Test.size();i++){
				String longest_match=null;
				String curr_subnet=null;
				//All possible subnet mask
				for(int j=subnet.size()-1;j>=0;j--){
					String key = Ip.getkey(Test.get(i),subnet.get(j));
					//IP lookup
					if(forwarding_table.containsKey(key)){
						value val = forwarding_table.get(key);
						String actual_key = Ip.getkey(Test.get(i),val.subnet_mask);
						if(actual_key.equals(key)){
							longest_match=key;
							curr_subnet=subnet.get(j);
							break;
						}
						
					}
				}
				//IP look up not matched
				if(longest_match==null){
					//System.out.println("not matched"+Test.get(i));
					def_count+=1;
					out.add("255.255.255.255");
				}else{
					//System.out.println("Longest_prefix :: "+longest_match+" value:: "+Test.get(i)+" subnet:: "+curr_subnet);
					match_Count+=1;
					out.add(longest_match);
				}
			}
			long endTime = System.currentTimeMillis();
			timeTaken+= endTime-startTime;
			if(first_flag){
				first_flag=false;
				System.out.println("Naive Time taken (1 iterations) :: "+timeTaken+"ms");
				System.out.println("Mathced :: "+match_Count+" default count ::"+def_count); 
				System.out.println("*********************************************************");
			}
			
		 }
		 
		 System.out.println("Naive Time taken (100 iterations) :: "+timeTaken+"ms");
		 System.out.println("Mathced :: "+match_Count+" default count ::"+def_count);
		 System.out.println("*********************************************************");
		 // Writing result into Output file
		 try {
			PrintWriter pw = new PrintWriter("C:\\Users\\ritvi\\workspace\\algos\\ComputerNetworks\\src\\lookup\\Naive_out","UTF-8");
			for(int i=0;i<Test.size();i++){
				pw.println("IP :: "+Test.get(i)+" Nexthop :: "+out.get(i));
			}
			pw.close();
		 
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		 
		 
	}
	/*
	 * Trie lookup
	 * Write result into Output file
	 */
	public void trie_lookup(ArrayList<String> Test){
		@SuppressWarnings("unused")
		int counter=0;
		boolean first_flag=true;
		trie lp_trie =  new trie();
		//long startTime = System.currentTimeMillis();
		for(int j=0;j<100;j++){	
			for(int i=0;i<Test.size();i++){
				//String result=lp_trie.trie_lookup(Test.get(i));
				lp_trie.trie_lookup(Test.get(i));
				//break;
				/*if(!result.equals("def")){
					counter+=1;
				}
				out.add(result);
				 */
			}
			if(first_flag){
				first_flag=false;
				System.out.println("Trie Time taken(1 iterations):: " +lp_trie.time+"ms");
				System.out.println("Mathced :: "+lp_trie.count+" default count ::"+(Test.size()-lp_trie.count));
				System.out.println("*********************************************************");
			}
			
		}
		//long endTime = System.currentTimeMillis();
		//long takenTime = endTime-startTime;
		int takenTime = lp_trie.time;
		//System.out.println("Start Time:: "+startTime+" End Time:: "+endTime+" Trie Time taken :: "+takenTime);
		System.out.println("Trie Time taken(100 iterations):: " + takenTime+"ms");
		System.out.println("Mathced :: "+lp_trie.count+" default count ::"+(Test.size()*100-lp_trie.count));
		System.out.println("*********************************************************");
		try {
			PrintWriter pw = new PrintWriter("C:\\Users\\ritvi\\workspace\\algos\\ComputerNetworks\\src\\lookup\\Trie_out","UTF-8");
			for(int i=0;i<Test.size();i++){
				pw.println("IP :: "+Test.get(i)+" Nexthop :: "+lp_trie.out.get(i));
			}
			pw.close();
		 
		 
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	/*
	 * Main function
	 * Read Data from input file and test file
	 * Calls lookup functions
	 */
	@SuppressWarnings({ "resource", "unused" })
	public static void main(String args[]) throws FileNotFoundException{
		Iplookup Ip = new Iplookup();
		File filename = new File(args[0]);
		Scanner in = new Scanner(filename);
		ArrayList<String> all_subnet = new ArrayList<String>();
		ArrayList<String> Test = new ArrayList<String>();
		all_subnet = Ip.get_all_subnet_mask();
		//System.out.println(all_subnet);
		trie lp_trie =  new trie();
		String temp="";
		int count=0;
		//Reading Input Data 
		while(in.hasNext()){	
			String input= in.next();
			String buffer[] = input.split("/");
			int CIDR = Integer.parseInt(buffer[1]);
			String subnet_mask =Ip.get_subnet_mask(CIDR);
			String net_prefix=Ip.getkey(buffer[0],subnet_mask);
			//System.out.println(input);
			//System.out.println("Network prefix:: "+net_prefix);
			//System.out.println("subnet mask   :: "+subnet_mask);
			forwarding_table.put(net_prefix,new value(buffer[0],subnet_mask));
			
			String bin_prefix= Ip.get_bin_prefix(buffer[0],CIDR);
			lp_trie.insert(bin_prefix,buffer[0]);
			
		}
		//Reading Test data
		File testfilename = new File(args[1]);
		Scanner test = new Scanner(testfilename);
		while(test.hasNext()){
			Test.add(test.next());
		}
		//calling lookup function
		Ip.trie_lookup(Test);
		Ip.naive_lookup(Test,all_subnet);
	}
}

/*
 * Trie Tree Node
 * Represents tree structure and values
 */
class node{
	node left;
	node right;
	String val;
	boolean ind;
	String next_hop;
	String prefix;
	
	node(node left, node right,String val, boolean ind, String prefix,String next_hop){
		this.left=left;
		this.right=right;
		this.ind=ind;
		this.val=val;
		this.next_hop =next_hop;
		this.prefix=prefix;
		
	}
	
}

/*
 * Trie Class 
 * Create Binary tree to store network prefix in Bit format
 * Perform lookup operation
 * Display Tree
 * Tree Insertion
 */

class trie{
	public static node root=new node(null,null,"R",false,null,null);
	public ArrayList<String> out = new ArrayList<String>();
	public int time=0;
	public int count=0;
	
	/*
	 * Insert Ip address to Binary Tree
	 */
	public void insert(String value,String next_hop){
		String bin_val=value;
		
		node  temp =root;
		//System.out.println("bin string:: "+bin_val+" length:: "+bin_val.length());
		char []bin_val_array = bin_val.toCharArray();
		
		for (int i=0;i<bin_val_array.length;i++){
			if(bin_val_array[i] == '1'){
				if( temp.right!=null){
					temp=temp.right;
				}else{
					temp.right = new node(null,null,"1",false,null,null); 
					temp=temp.right;
				}
			}
			else{
				if(temp.left!=null){
					temp=temp.left;
				}
				else{
					temp.left=new node(null,null,"0",false,null,null);
					temp=temp.left;
				}
			}
		}
		temp.ind=true;
		temp.next_hop=next_hop;
		temp.prefix = value;
		//display(root);
	}
	
	/*
	 *Display Trie Tree 
	 */
	public void display(node root){
			System.out.println(" "+root.val);
			if(root.ind){
				System.out.println("Next hop::"+root.next_hop);
			}
			if(root.left!=null){
				display(root.left);
			}
			if(root.right!=null){
				display(root.right);
			}
			
	}
	
	/*
	 * Perform Trie Lookup operation
	 * finds Longest prefix Match for the binary input string present in the tree 
	 */
	public void trie_lookup(String input){
		//System.out.println("Find String:: "+input);
		String buffer[] = input.split("\\.");
		String bin_val="";
		
		for(int i=0;i<buffer.length;i++){
				bin_val+=String.format("%8s", Integer.toBinaryString(Integer.parseInt(buffer[i]))).replace(' ', '0');
		}
		
		node  temp =root;
		//System.out.println("bin string:: "+bin_val+" length:: "+bin_val.length());
		String longest_match=null;
		String next_hop=null;
		char []bin_val_array = bin_val.toCharArray();
		long st = System.currentTimeMillis();
		for (int i=0;i<bin_val_array.length;i++){
			
			
			if(bin_val_array[i] == '1'){
				if( temp.right!=null){
					//System.out.println("Going right..."+bin_val_array[i]);
					temp=temp.right;
				}else{
					break;
				}
			}
			else{
				if(temp.left!=null){
					//System.out.println("Going left..."+bin_val_array[i]);
					temp=temp.left;
				}
				else{
					break;
				}
			}
			//System.out.println("checking ind..."+temp.ind);
			if(temp.ind){
				longest_match = temp.prefix;
				next_hop=temp.next_hop;
			}
		}
		long et = System.currentTimeMillis();
		time+=(et-st);
		if(longest_match==null){
			//System.out.println("value:: "+input);
			out.add("255.255.255.255");
			//return "def";
		}
		else{
			out.add(next_hop);
			count+=1;
		}
		//return next_hop;
		//System.out.println("Longest prefix match :: "+longest_match+" Nexthop:: "+next_hop);
		
	}
	
	

}
	




