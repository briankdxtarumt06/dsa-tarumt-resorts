package tarumtresort.entity;

public class Guest {
    private String guestId;
    private String name;
    private String icOrPassport;
    private String contactNumber;
    private String nationality;
    private String address;

    //Constructor 
    public Guest(String guestId, String name, String icOrPassport, String contactNumber, String nationality, String address){
        this.guestId = guestId;
        this.name = name;
        this.icOrPassport = icOrPassport;
        this.contactNumber = contactNumber;
        this.nationality = nationality;
        this.address = address;
    }

    //setters 
    public void setGuestId(String guestId){ this.guestId = guestId; }
    public void setName (String name){ this.name = name; }
    public void setIcOrPassport (String icOrPassport){ this.icOrPassport = icOrPassport; }
    public void setContactNumber (String contactNumber){ this.contactNumber = contactNumber; }
    public void setNationality (String nationality){ this.nationality = nationality; }
    public void setAdress (String address){ this.address = address; }

    //getters 
    public String getIC(){ return guestId; }
    public String getName(){ return name;}
    public String getIcOrPassport(){ return icOrPassport; }
    public String getContactNumber(){ return contactNumber; }
    public String getNationality(){ return nationality; }
    public String getAddress(){ return address; }

    @Override
    public String toString() {
        return "Guest{" +
                "guestId='" + guestId + '\'' +
                ", name='" + name + '\'' +
                ", icOrPassport='" + icOrPassport + '\'' +
                ", contactNumber='" + contactNumber + '\'' +
                ", nationality='" + nationality + '\'' +
                ", address='" + address + '\'' +
                '}';
    }

}
