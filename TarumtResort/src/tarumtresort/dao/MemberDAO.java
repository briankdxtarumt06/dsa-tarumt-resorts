package tarumtresort.dao;

import java.nio.file.Path;
import tarumtresort.adt.LinkedList;
import tarumtresort.entity.Member;
import tarumtresort.utility.JsonFileHandler;

public class MemberDAO {
    private final String FILE_NAME = "data/members.json";
    private final LinkedList<Member> members = new LinkedList<>();

    public void Add(Member member) {
        members.addSorted(member);
    }
    
    public void Remove(String memberId) {
        LinkedList<Member> kept = new LinkedList<>();
        for (int i = 0; i < members.size(); i++) {
            Member m = members.get(i);
            if (!m.getMemberId().equals(memberId)) {
                kept.addBack(m);
            }
        }
        members.clear();
        for (int i = 0; i < kept.size(); i++) {
            members.addBack(kept.get(i));
        }
    }

    public Member FindById(String memberId) {
        for (int i = 0; i < members.size(); i++) {
            if (members.get(i).getMemberId().equals(memberId)) {
                return members.get(i);
            }
        }
        return null;
    }

    public LinkedList<Member> GetAll() {
        return members;
    }

    public int Size() {
        return members.size();
    }

    public boolean IsEmpty() {
        return members.isEmpty();
    }

    public LinkedList<Member> LoadFromFile() {
        members.clear();
        try {
            LinkedList<Member> loaded = JsonFileHandler.loadList(Path.of(FILE_NAME), Member.class);
            for (int i = 0; i < loaded.size(); i++) {
                members.addBack(loaded.get(i));
            }
        } catch (java.io.IOException e) {
            System.err.println("Failed to load " + FILE_NAME + ": " + e.getMessage());
        }
        return members;
    }

    public void SaveToFile() {
        try {
            JsonFileHandler.saveList(members, Path.of(FILE_NAME));
        } catch (java.io.IOException e) {
            System.err.println("Failed to save " + FILE_NAME + ": " + e.getMessage());
        }
    }
}
