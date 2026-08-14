package tarumtresort.dao;

import java.io.IOException;
import java.nio.file.Path;
import tarumtresort.adt.LinkedList;
import tarumtresort.adt.LinkedListInterface;
import tarumtresort.entity.Inquiry;
import tarumtresort.utility.JsonFileHandler;

/**
 *
 * @author Wen Ling
 */
public class InquiryDAO {

    private static final Path PENDING_FILE = Path.of("data/pendingInquiries.json");
    private static final Path RESOLVED_FILE = Path.of("data/resolvedInquiries.json");

    public void savePendingInquiryList(LinkedListInterface<Inquiry> pendingInquiryList) {
        try {
            JsonFileHandler.saveList(pendingInquiryList, PENDING_FILE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveResolvedInquiryList(LinkedListInterface<Inquiry> resolvedInquiryList) {
        try {
            JsonFileHandler.saveList(resolvedInquiryList, RESOLVED_FILE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public LinkedList<Inquiry> retrievePendingInquiryList() {
        try {
            return JsonFileHandler.loadList(PENDING_FILE, Inquiry.class);
        } catch (IOException e) {
            e.printStackTrace();
            return new LinkedList<>();
        }
    }

    public LinkedList<Inquiry> retrieveResolvedInquiryList() {
        try {
            return JsonFileHandler.loadList(RESOLVED_FILE, Inquiry.class);
        } catch (IOException e) {
            e.printStackTrace();
            return new LinkedList<>();
        }
    }

    public Inquiry getPendingInquiryById(String inquiryId) {
        LinkedListInterface<Inquiry> pendingInquiryList = retrievePendingInquiryList();
        for (int i = 0; i < pendingInquiryList.size(); i++) {
            if (pendingInquiryList.get(i).getInquiryId().equals(inquiryId)) {
                return pendingInquiryList.get(i);
            }
        }
        return null;
    }
}