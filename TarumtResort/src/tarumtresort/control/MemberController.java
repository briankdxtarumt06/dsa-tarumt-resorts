package tarumtresort.control;

import tarumtresort.adt.*;
import tarumtresort.dao.*;
import tarumtresort.entity.*;

public class MemberController {
    public final MemberDAO memberDAO = new MemberDAO();
    public final RewardDAO rewardDAO = new RewardDAO();
    public final NotificationDAO notificationDAO = new NotificationDAO();
    public final RedemptionRecordDAO redemptionRecordDAO = new RedemptionRecordDAO();
    public final PointTransactionDAO pointTransactionDAO = new PointTransactionDAO();

    public LinkedListInterface<Member> memberList = memberDAO.GetAll();
    public LinkedListInterface<Reward> rewardList = rewardDAO.GetAll();
    public LinkedListInterface<Notification> notificationList = notificationDAO.GetAll();
    public LinkedListInterface<RedemptionRecord> redemptionRecordList = redemptionRecordDAO.GetAll();
    public LinkedListInterface<PointTransaction> pointTransactionList = pointTransactionDAO.GetAll();

    public MemberController() {
        LoadAll();
    }

    public void LoadAll() {
        memberDAO.LoadFromFile();
        rewardDAO.LoadFromFile();
        notificationDAO.LoadFromFile();
        redemptionRecordDAO.LoadFromFile();
        pointTransactionDAO.LoadFromFile();
    }

    public void SaveAll() {
        memberDAO.SaveToFile();
        rewardDAO.SaveToFile();
        notificationDAO.SaveToFile();
        redemptionRecordDAO.SaveToFile();
        pointTransactionDAO.SaveToFile();
    }
}
