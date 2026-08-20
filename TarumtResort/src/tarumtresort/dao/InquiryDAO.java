package tarumtresort.dao;

import java.io.IOException;
import java.nio.file.Path;
import tarumtresort.adt.LinkedList;
import tarumtresort.adt.LinkedListInterface;
import tarumtresort.entity.Inquiry;
import tarumtresort.entity.enums.InquiryStatus;
import tarumtresort.utility.JsonFileHandler;

/**
 *
 * @author Wen Ling
 */
public class InquiryDAO {

    private static final Path INQUIRY_FILE = Path.of("data/inquiries.json");

    public void saveInquiryList(LinkedListInterface<Inquiry> inquiryList) {
        try {
            JsonFileHandler.saveList(inquiryList, INQUIRY_FILE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public LinkedList<Inquiry> retrieveInquiryList() {
        try {
            return JsonFileHandler.loadList(INQUIRY_FILE, Inquiry.class);
        } catch (IOException e) {
            e.printStackTrace();
            return new LinkedList<>();
        }
    }

    public Inquiry getPendingInquiryById(String inquiryId) {
        LinkedListInterface<Inquiry> inquiryList = retrieveInquiryList();
        for (int i = 0; i < inquiryList.size(); i++) {
            Inquiry inq = inquiryList.get(i);
            if (inq.getStatus() == InquiryStatus.PENDING && inq.getInquiryId().equals(inquiryId)) {
                return inq;
            }
        }
        return null;
    }
}