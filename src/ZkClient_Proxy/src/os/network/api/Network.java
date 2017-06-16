package os.network.api;

public interface Network {
	public String getHostInfo();
	public String call(String namespace,String method,Object[] args);
	
}
