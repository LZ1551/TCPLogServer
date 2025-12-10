import java.io.Serializable;

public class Message implements Serializable {
    private String macAddress;
    private String message;

    public Message(String macAddress, String message){
        this.macAddress = macAddress;
        this.message = message;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public String getMessage(){
        return message;
    }
}
