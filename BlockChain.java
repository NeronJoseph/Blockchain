import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.io.*;

class AccountVector
{
	int accountNo;
	int startBal;
	Vector<AccountChain> a;

	public AccountVector(int n,int b)
	{
		accountNo=n;
		startBal=b;
		a=new Vector<AccountChain>(1,1);
	}
}
class AccountChain
{
	String prevSha1;
	int src,dest,amt;

	public AccountChain(String a, int b, int c, int d)
	{
		prevSha1=a;
		src=b;
		dest=c;
		amt=d;
	}
}

class ParticipatingServer
{
	Vector<AccountVector> v;
	int id;

	public ParticipatingServer(int i)
	{
		id=i;
		v=new Vector<AccountVector>(1,1);
	}
	int verifyTransaction(int s, int d, int a) throws NoSuchAlgorithmException
	{
		AccountVector av;
		av=v.get(s);
		Vector<AccountChain> ac;
		AccountChain chain;
		ac=av.a;
		int bal=av.startBal;
		String oldHash="";
		for(int i=0;i<ac.size();i++)
		{
			chain=ac.get(i);
			if(!oldHash.equals(chain.prevSha1))
				return 0;
			//ac=av.get(i);
			if(chain.src==s)
				bal-=chain.amt;
			else if(chain.dest==s)
					bal+=chain.amt;
			oldHash=new HashValue().sha1(chain.prevSha1+","+chain.src+","+chain.dest+","+chain.amt);
		}
		if(bal<a)
			return 0;
		return 1;
	}

	int printBalance(int s)
	{
		AccountVector av;
		av=v.get(s);
		Vector<AccountChain> ac;
		AccountChain chain;
		ac=av.a;
		int bal=av.startBal;
		String oldHash="";
		for(int i=0;i<ac.size();i++)
		{
			chain=ac.get(i);
			if(chain.src==s)
				bal-=chain.amt;
			else if(chain.dest==s)
					bal+=chain.amt;
		}
		return bal;
}

	void addTransaction(int s, int d, int a) throws NoSuchAlgorithmException
	{
		AccountVector av;
		av=v.get(s);
		Vector<AccountChain> ac;
		AccountChain chain;
		ac=av.a;
		String prevHash="";
		if(ac.size()>0)
		{
			chain=ac.get(ac.size()-1);
			prevHash=new HashValue().sha1(chain.prevSha1+","+chain.src+","+chain.dest+","+chain.amt);
		}
		AccountChain newChain=new AccountChain(prevHash,s,d,a);
		ac.add(newChain);
		av=v.get(d);
		ac=av.a;
		prevHash="";
		if(ac.size()>0)
		{
			chain=ac.get(ac.size()-1);
			prevHash=new HashValue().sha1(chain.prevSha1+","+chain.src+","+chain.dest+","+chain.amt);
		}
		newChain=new AccountChain(prevHash,s,d,a);
		ac.add(newChain);
	}
}
class HashValue {

    /**
     * @param args
     * @throws NoSuchAlgorithmException
     */

    String sha1(String input) throws NoSuchAlgorithmException {
        MessageDigest mDigest = MessageDigest.getInstance("SHA1");
        byte[] result = mDigest.digest(input.getBytes());
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < result.length; i++) {
            sb.append(Integer.toString((result[i] & 0xff) + 0x100, 16).substring(1));
        }

        return sb.toString();
    }

	boolean verifySha1(String input, String testSha1) throws NoSuchAlgorithmException {
		return testSha1.equals(sha1(input));
	}
}

class PrintToFile
{
	void printServer(ParticipatingServer ps) throws IOException
	{
		PrintWriter writer = new PrintWriter("server"+ps.id, "UTF-8");
		//System.out.println(ps.id);
		AccountVector av;
		for(int i=0;i<ps.v.size();i++)
		{
			av=ps.v.get(i);
			writer.println(av.accountNo+","+av.startBal+","+av.a.size());
			Vector<AccountChain> vac;
			vac=av.a;
			AccountChain ac;
			for(int j=0;j<vac.size();j++)
			{
				ac=vac.get(j);
				writer.println(ac.prevSha1+","+ac.src+","+ac.dest+","+ac.amt);
			}
		}
		writer.close();
	}
}

public class BlockChain {
		static ParticipatingServer[] ps;
		static void transact(int s,int d,int a) throws NoSuchAlgorithmException
		{
			int proceed=0;
			for(int i=0;i<5;i++)
				proceed+=ps[i].verifyTransaction(s,d,a);
			if(proceed>=3)
			{
				System.out.println("Transaction accepted");
				for(int i=0;i<5;i++)
					ps[i].addTransaction(s,d,a);
				}
				else
				System.out.println("Transaction rejected");
		}
	    public static void main(String[] args) throws NoSuchAlgorithmException,IOException {
			//int nServers=5;
			ps=new ParticipatingServer[5];
			BufferedReader br;
			String line,oldHash;
			for(int i=0;i<5;i++)
			{
				br = new BufferedReader(new FileReader("server"+i));
				ps[i]=new ParticipatingServer(i);
				for(int j=0;j<6;j++)
				{
					line=br.readLine();
					oldHash="";
					String newHash;
					String[] parts=line.split(",");
					ps[i].v.add(new AccountVector(Integer.parseInt(parts[0]),Integer.parseInt(parts[1])));
					int size=Integer.parseInt(parts[2]);
					for(int k=0;k<size;k++)
					{
						line=br.readLine();
						parts=line.split(",");
						newHash=parts[0];
						if(!oldHash.equals(newHash))
						{
							System.out.println("Validation error in server "+i);
							//System.out.println("<"+oldHash+"><"+newHash+">");
							return;
						}
					//	oldHash=newHash;
						oldHash=new HashValue().sha1(line);
						ps[i].v.get(j).a.add(new AccountChain(parts[0],Integer.parseInt(parts[1]),Integer.parseInt(parts[2]),Integer.parseInt(parts[3])));
					}
				}
				br.close();
			}
			int src,dest,amt;
			br=new BufferedReader(new InputStreamReader(System.in));
			System.out.println("Source Account:");
			src=Integer.parseInt(br.readLine());
			System.out.println("Destination Account:");
			dest=Integer.parseInt(br.readLine());
			System.out.println("Amount:");
			amt=Integer.parseInt(br.readLine());
			transact(src,dest,amt);
			/*transact(0,2,100);
			transact(0,3,10);
			transact(0,4,100);
			transact(1,4,1000);
			transact(3,4,1000);
			transact(2,3,1000);*/
			for(int i=0;i<5;i++)
				new PrintToFile().printServer(ps[i]);
			System.out.println("ACCOUNT BALANCE");
			for(int i=0;i<6;i++)
				System.out.println("Account "+i+" - "+ps[0].printBalance(i));
        //System.out.println(new HashValue().sha1("test string to sha1"));
		//System.out.println(new HashValue().verifySha1("test string to sha1","bd4c24bb656d161a66d89a45f50bf52cb7134851"));
    }
}
