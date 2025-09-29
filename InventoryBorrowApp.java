// InventoryBorrowApp.java
// เมนู: เพิ่ม/ลบ/ค้นหา/รายการทั้งหมด + ยืม/คืนอุปกรณ์
// เงื่อนไขสำคัญ: ถ้าเพิ่มชื่ออุปกรณ์ซ้ำ → ไม่สร้าง object ใหม่ แต่เพิ่มจำนวนเข้าไปในรายการเดิม

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/* ========== MODEL (OOP) ========== */
abstract class Item {                         // Inheritance base
    private final int id;                     // Encapsulation
    private final String name;

    protected Item(int id, String name) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name required");
        this.id = id;
        this.name = name.trim();
    }
    public final int getId() { return id; }
    public final String getName() { return name; }

    public abstract String type();            // Polymorphism (override ในคลาสลูก)
    @Override public String toString() {
        return String.format("#%d [%s] %s", id, type(), name);
    }
}

class Equipment extends Item {                // คลาสหลักของโปรเจกต์
    private int total;                        // ทั้งหมด
    private int available;                    // คงเหลือให้ยืม

    public Equipment(int id, String name, int quantity) {
        super(id, name);
        if (quantity < 0) throw new IllegalArgumentException("quantity >= 0");
        this.total = quantity;
        this.available = quantity;
    }
    @Override public String type() { return "Equipment"; }

    public int getTotal() { return total; }
    public int getAvailable() { return available; }
    public int getBorrowed() { return total - available; }

    public void addQuantity(int q) {
        if (q <= 0) throw new IllegalArgumentException("quantity to add must be > 0");
        total += q; available += q;
    }
    public boolean borrow(int qty) {
        if (qty <= 0) return false;
        if (qty > available) return false;
        available -= qty; return true;
    }
    public void giveBack(int qty) {
        if (qty <= 0) throw new IllegalArgumentException("qty > 0");
        if (available + qty > total) throw new IllegalStateException("คืนเกินจำนวนที่ยืมค้าง");
        available += qty;
    }

    @Override public String toString() {
        return String.format("#%d %s — total=%d, available=%d", getId(), getName(), total, available);
    }
}

/* ========== Repository ========== */
class EquipmentRepository {
    private final Map<Integer, Equipment> store = new LinkedHashMap<>();
    private int nextId = 1001;

    public int nextId() { return nextId++; }
    public Equipment save(Equipment e) { store.put(e.getId(), e); return e; }
    public boolean delete(int id) { return store.remove(id) != null; }
    public Optional<Equipment> findById(int id) { return Optional.ofNullable(store.get(id)); }
    public List<Equipment> findAll() { return new ArrayList<>(store.values()); }

    public Optional<Equipment> findByNameExactIgnoreCase(String name) {
        if (name == null) return Optional.empty();
        String key = name.trim().toLowerCase();
        for (Equipment e : store.values()) {
            if (e.getName().toLowerCase().equals(key)) return Optional.of(e);
        }
        return Optional.empty();
    }

    public List<Equipment> searchByName(String kw) {
        String key = (kw==null?"":kw).toLowerCase();
        List<Equipment> out = new ArrayList<>();
        for (Equipment e : store.values()) {
            if (e.getName().toLowerCase().contains(key)) out.add(e);
        }
        return out;
    }
}

/* ========== Borrow/Return Log ========== */
class Tx {
    private final String studentId;
    private final int itemId;
    private final String itemName;
    private int qty;                           // จำนวนในบรรทัดนี้ (อาจถูกแยกตอนคืนบางส่วน)
    private final LocalDateTime borrowAt;
    private LocalDateTime returnAt;            // null = ยังไม่คืน (open)

    public Tx(String sid, int itemId, String itemName, int qty, LocalDateTime at) {
        this.studentId = sid; this.itemId = itemId; this.itemName = itemName;
        this.qty = qty; this.borrowAt = at; this.returnAt = null;
    }
    public String getStudentId(){ return studentId; }
    public int getItemId(){ return itemId; }
    public String getItemName(){ return itemName; }
    public int getQty(){ return qty; }
    public LocalDateTime getBorrowAt(){ return borrowAt; }
    public LocalDateTime getReturnAt(){ return returnAt; }
    public boolean isOpen(){ return returnAt == null; }
    public void closeAll(LocalDateTime when){ this.returnAt = when; }
    public static String fmt(LocalDateTime dt){
        return dt==null? "" : dt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
    @Override public String toString(){
        String status = isOpen()? "OPEN" : "CLOSED";
        return String.format("%-10s | #%d %-22s | %3d | %s | %s | %s",
                studentId, itemId, itemName, qty, fmt(borrowAt), fmt(returnAt), status);
    }
}

/* ========== Services ========== */
class EquipmentService {
    static class AddResult {
        final Equipment equipment;
        final boolean updatedExisting; // true = พบชื่อซ้ำและเพิ่มจำนวน, false = สร้างใหม่
        AddResult(Equipment e, boolean updated){ this.equipment = e; this.updatedExisting = updated; }
    }

    private final EquipmentRepository repo;
    public EquipmentService(EquipmentRepository repo) { this.repo = repo; }

    // เพิ่มด้วย “ชื่อ + จำนวน” เท่านั้น
    public AddResult addOrIncrease(String name, int qty) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name required");
        if (qty <= 0) throw new IllegalArgumentException("quantity > 0");

        // ตรวจสอบว่ามีชื่อซ้ำหรือไม่ (ไม่สนตัวพิมพ์เล็กใหญ่)
        var dup = repo.findByNameExactIgnoreCase(name);
        if (dup.isPresent()) {
            Equipment e = dup.get();
            e.addQuantity(qty);                 // เพิ่มจำนวนใน object เดิม
            return new AddResult(e, true);      // อัปเดตรายการเดิม
        }

        // ถ้าไม่มีชื่อซ้ำ → สร้างใหม่
        int id = repo.nextId();
        Equipment e = new Equipment(id, name.trim(), qty);
        repo.save(e);
        return new AddResult(e, false);
    }

    public boolean deleteItem(int id) { return repo.delete(id); }
    public List<Equipment> listAll() { return repo.findAll(); }
    public List<Equipment> search(String kw) { return repo.searchByName(kw); }
    public Optional<Equipment> find(int id) { return repo.findById(id); }
}

class BorrowService {
    private final EquipmentService equipmentService;
    private final List<Tx> txs = new ArrayList<>();

    public BorrowService(EquipmentService service){ this.equipmentService = service; }

    public String borrow(String studentId, int itemId, int qty){
        var e = equipmentService.find(itemId).orElse(null);
        if (e == null) return "ไม่พบอุปกรณ์";
        if (!e.borrow(qty)) return "ยืมไม่ได้ (คงเหลือไม่พอ)";
        txs.add(new Tx(studentId, itemId, e.getName(), qty, LocalDateTime.now()));
        return "ยืมสำเร็จ";
    }

    // จับคู่คืนกับบรรทัดล่าสุดก่อน (LIFO) และรองรับ “คืนบางส่วน”
    public String giveBack(String studentId, int itemId, int qty){
        var e = equipmentService.find(itemId).orElse(null);
        if (e == null) return "ไม่พบอุปกรณ์";
        try {
            e.giveBack(qty);
        } catch (Exception ex) {
            return "คืนไม่ได้: " + ex.getMessage();
        }
        int toClose = qty;
        LocalDateTime now = LocalDateTime.now();
        for (int i = txs.size()-1; i>=0 && toClose>0; i--){
            Tx t = txs.get(i);
            if (!t.isOpen()) continue;
            if (t.getItemId()!=itemId) continue;
            if (!t.getStudentId().equals(studentId)) continue;

            if (t.getQty() <= toClose){
                toClose -= t.getQty();
                t.closeAll(now);               // ปิดทั้งบรรทัด
            } else {
                // แตกบรรทัด: ปิดบางส่วน
                int remain = t.getQty() - toClose;
                Tx closed    = new Tx(t.getStudentId(), t.getItemId(), t.getItemName(), toClose, t.getBorrowAt());
                closed.closeAll(now);
                Tx stillOpen = new Tx(t.getStudentId(), t.getItemId(), t.getItemName(), remain, t.getBorrowAt());
                txs.remove(i);
                txs.add(i, closed);
                txs.add(i, stillOpen);
                toClose = 0;
            }
        }
        return "คืนสำเร็จ";
    }

    public List<Tx> listAllTx(){ return txs; }
    public List<Tx> listOpenTx(){
        List<Tx> out = new ArrayList<>();
        for (Tx t: txs) if (t.isOpen()) out.add(t);
        return out;
    }
    public List<Tx> listByStudent(String sid){
        List<Tx> out = new ArrayList<>();
        for (Tx t: txs) if (t.getStudentId().equalsIgnoreCase(sid)) out.add(t);
        return out;
    }
}

/* ========== CLI (Menu) ========== */
public class InventoryBorrowApp {
    private final Scanner sc = new Scanner(System.in);
    private final EquipmentService equipmentService = new EquipmentService(new EquipmentRepository());
    private final BorrowService borrowService = new BorrowService(equipmentService);

    public static void main(String[] args) { new InventoryBorrowApp().run(); }

    private void seed(){
       
    }

    private void run(){
        seed();
        while(true){
            printMenu();
            System.out.print("เลือกเมนู: ");
            String c = sc.nextLine().trim();
            switch (c){
                case "1" -> doAdd();
                case "2" -> doDelete();
                case "3" -> doSearch();
                case "4" -> doList();
                case "5" -> doBorrow();
                case "6" -> doReturn();
                case "7" -> showTx();
                case "8" -> { System.out.println("จบโปรแกรม!!!"); return; }
                default -> System.out.println("เมนูไม่ถูกต้อง");
            }
            System.out.println();
        }
    }

    private void printMenu(){
        System.out.println("=== Equipment Menu ===");
        System.out.println("1) เพิ่มรายการ (ชื่อ + จำนวน) — ถ้าชื่อซ้ำจะเพิ่มจำนวนให้รายการเดิม");
        System.out.println("2) ลบรายการ");
        System.out.println("3) ค้นหารายการ");
        System.out.println("4) รายการทั้งหมด");
        System.out.println("5) ยืม อุปกรณ์");
        System.out.println("6) คืน อุปกรณ์");
        System.out.println("7) ดูประวัติการยืม-คืน");
        System.out.println("8) จบโปรแกรม");
    }

    private void doAdd(){
        try{
            System.out.print("ชื่ออุปกรณ์: ");
            String name = sc.nextLine().trim();
            System.out.print("จำนวนที่เพิ่ม: ");
            int qty = Integer.parseInt(sc.nextLine().trim());

            EquipmentService.AddResult res = equipmentService.addOrIncrease(name, qty);
            if (res.updatedExisting) {
                System.out.println("พบชื่อซ้ำ → อัปเดตจำนวนให้รายการเดิมแล้ว");
            } else {
                System.out.println("สร้างรายการใหม่เรียบร้อย");
            }
            System.out.println("สถานะปัจจุบัน: " + res.equipment);
        }catch(Exception ex){
            System.out.println("เพิ่มไม่สำเร็จ: " + ex.getMessage());
        }
    }

    private void doDelete(){
        try{
            System.out.print("รหัส (#id): ");
            int id = Integer.parseInt(sc.nextLine().trim());
            boolean ok = equipmentService.deleteItem(id);
            System.out.println(ok? "ลบสำเร็จ" : "ไม่พบรายการ");
        }catch(NumberFormatException nfe){
            System.out.println("รหัสต้องเป็นตัวเลข");
        }
    }

    private void doSearch(){
        System.out.print("คำค้นชื่อ: ");
        String kw = sc.nextLine();
        var result = equipmentService.search(kw);
        if (result.isEmpty()) System.out.println("ไม่พบรายการ");
        else result.forEach(System.out::println);
    }

    private void doList(){
        var all = equipmentService.listAll();
        if (all.isEmpty()) System.out.println("ยังไม่มีรายการ");
        else all.forEach(System.out::println);
    }

    private void doBorrow(){
        try{
            System.out.print("รหัสนิสิต: ");
            String sid = sc.nextLine().trim();
            System.out.print("รหัสอุปกรณ์: ");
            int id = Integer.parseInt(sc.nextLine().trim());
            System.out.print("จำนวน: ");
            int qty = Integer.parseInt(sc.nextLine().trim());
            String msg = borrowService.borrow(sid, id, qty);
            System.out.println(msg);
        }catch(NumberFormatException nfe){
            System.out.println("กรอกตัวเลขให้ถูกต้อง");
        }
    }

    private void doReturn(){
        try{
            System.out.print("รหัสนิสิต: ");
            String sid = sc.nextLine().trim();
            System.out.print("รหัสอุปกรณ์: ");
            int id = Integer.parseInt(sc.nextLine().trim());
            System.out.print("จำนวน: ");
            int qty = Integer.parseInt(sc.nextLine().trim());
            String msg = borrowService.giveBack(sid, id, qty);
            System.out.println(msg);
        }catch(NumberFormatException nfe){
            System.out.println("กรอกตัวเลขให้ถูกต้อง");
        }
    }

    private void showTx(){
        System.out.println("ดูประวัติ: 1) all  2) open  3) by student");
        String m = sc.nextLine().trim();
        List<Tx> list = switch (m){
            case "2" -> borrowService.listOpenTx();
            case "3" -> { System.out.print("รหัสนิสิต: "); yield borrowService.listByStudent(sc.nextLine().trim()); }
            default  -> borrowService.listAllTx();
        };
        if (list.isEmpty()){ System.out.println("ไม่มีประวัติ"); return; }
        System.out.println("studentId | #id ชื่ออุปกรณ์            | qty | borrow_at          | return_at          | status");
        System.out.println("-------------------------------------------------------------------------------------------------");
        for (Tx t: list) System.out.println(t);
    }
}