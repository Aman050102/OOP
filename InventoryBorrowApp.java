import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/* ========== MODEL (OOP) ========== */
abstract class Item {
    // Encapsulation: ซ่อนฟิลด์ไว้ภายใน (private) และ final เพื่อไม่ให้แก้ภายหลัง
    private final int id;
    private final String name;

    // กำหนดผ่าน constructor พร้อม validation
    protected Item(int id, String name) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name required");
        this.id = id;
        this.name = name.trim();
    }
    // ให้เข้าถึงด้วย getter เท่านั้น (ไม่เปิด setter -> ป้องกันการแก้ไขโดยตรง)
    public final int getId() { return id; }
    public final String getName() { return name; }

    // Abstraction + Polymorphism: บังคับให้คลาสลูกบอก "type" ของตนเอง
    public abstract String type();

    // Polymorphism (override ในคลาสลูกได้): รูปแบบการแสดงผลมาตรฐานของ Item
    @Override public String toString() {
        return String.format("#%d [%s] %s", id, type(), name);
    }
}

class Equipment extends Item {
    // Encapsulation: ฟิลด์สต็อกเป็น private และถูกปรับค่าได้ผ่านเมธอดที่มีกติกาเท่านั้น
    private int total;     // จำนวนทั้งหมดในคลัง
    private int available; // จำนวนพร้อมให้ยืม (total - ที่ถูกยืมค้าง)

    public Equipment(int id, String name, int quantity) {
        super(id, name);
        if (quantity < 0) throw new IllegalArgumentException("quantity >= 0");
        this.total = quantity;
        this.available = quantity;
    }
    // Polymorphism: บอกชนิดของ Item นี้
    @Override public String type() { return "Equipment"; }

    // Getter (อ่านอย่างเดียวจากภายนอก)
    public int getTotal() { return total; }
    public int getAvailable() { return available; }
    public int getBorrowed() { return total - available; }

    // เพิ่มสต็อก (มี validation) -> หลัก Encapsulation
    public void addQuantity(int q) {
        if (q <= 0) throw new IllegalArgumentException("quantity to add must be > 0");
        total += q; available += q;
    }

    // ลดจำนวนสต็อก (ห้ามลดจนต่ำกว่าที่ "ยืมค้าง") -> validation ภายในอ็อบเจกต์
    public void removeQuantity(int q) {
        if (q <= 0) throw new IllegalArgumentException("quantity to reduce must be > 0");
        if (q > total) throw new IllegalArgumentException("ลดเกินจำนวนทั้งหมด");
        int borrowed = getBorrowed();
        if (total - q < borrowed) {
            // ป้องกันข้อมูลผิด: ถ้าลดจนต่ำกว่า (ของที่ถูกยืม) => ผิดกติกา
            throw new IllegalStateException("ลดไม่ได้: จะทำให้สต็อกต่ำกว่าจำนวนที่ถูกยืมค้าง ("+borrowed+")");
        }
        total -= q;
        available = total - borrowed; // ให้ available สอดคล้องกับ total/borrowed เสมอ
    }

    // ยืม: ต้องมีของพร้อมให้ยืมพอ (available) และจำนวนต้อง > 0
    public boolean borrow(int qty) {
        if (qty <= 0) return false;
        if (qty > available) return false;
        available -= qty; return true;
    }

    // คืน: จำนวนต้อง > 0 และห้ามทำให้ available > total (ซึ่งแปลว่าคืนเกินที่ค้าง)
    public void giveBack(int qty) {
        if (qty <= 0) throw new IllegalArgumentException("qty > 0");
        if (available + qty > total) throw new IllegalStateException("คืนเกินจำนวนที่ยืมค้าง");
        available += qty;
    }

    @Override public String toString() {
        return String.format("#%d %s  ทั้งหมด = %d, คงเหลือ = %d", getId(), getName(), total, available);
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

    // หา "ชื่อซ้ำ" เพื่อรวมจำนวน (business rule ใน service จะเรียกใช้)
    public Optional<Equipment> findByNameExactIgnoreCase(String name) {
        if (name == null) return Optional.empty();
        String key = name.trim().toLowerCase();
        for (Equipment e : store.values()) {
            if (e.getName().toLowerCase().equals(key)) return Optional.of(e);
        }
        return Optional.empty();
    }

    // ค้นหาชื่อแบบ contains (search)
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
    private int qty;                 // จำนวนที่ยืมในบรรทัดนี้ (ถ้า OPEN = คงค้าง)
    private final LocalDateTime borrowAt;
    private LocalDateTime returnAt;  // null = ยังไม่คืน (OPEN)

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

    // ช่วยฟอร์แมตวันเวลาเวลาแสดงผล
    public static String fmt(LocalDateTime dt){
        return dt==null? "" : dt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    // แสดงผลในรูปแบบตารางอ่านง่าย
    @Override public String toString(){
        String status = isOpen()? "กำลังทำรายการ" : "เสร็จสิ้น";
        return String.format("%-10s | #%d %-22s | %3d | %s | %s | %s",
                studentId, itemId, itemName, qty, fmt(borrowAt), fmt(returnAt), status);
    }
}

/* ========== Services ========== */
class EquipmentService {
    // DTO ย่อย: บอกผล "เพิ่ม/อัปเดต"
    static class AddResult {
        final Equipment equipment;
        final boolean updatedExisting; // true = พบชื่อซ้ำและเพิ่มจำนวน, false = สร้างใหม่
        AddResult(Equipment e, boolean updated){ this.equipment = e; this.updatedExisting = updated; }
    }

    private final EquipmentRepository repo;
    public EquipmentService(EquipmentRepository repo) { this.repo = repo; }

    // เพิ่มรายการแบบ “รวมชื่อซ้ำ” ตามกติกา
    public AddResult addOrIncrease(String name, int qty) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name required");
        if (qty <= 0) throw new IllegalArgumentException("quantity > 0");
        var dup = repo.findByNameExactIgnoreCase(name);
        if (dup.isPresent()) {
            Equipment e = dup.get();
            e.addQuantity(qty);                  // รวมเข้ารายการเดิม
            return new AddResult(e, true);
        }
        int id = repo.nextId();
        Equipment e = new Equipment(id, name.trim(), qty);
        repo.save(e);
        return new AddResult(e, false);
    }

    // ลดจำนวนแบบกันผิด (ใช้ validation ภายใน Equipment.removeQuantity)
    public Equipment reduceQuantity(int id, int qty){
        Equipment e = repo.findById(id).orElseThrow(() -> new NoSuchElementException("ไม่พบรหัสนี้"));
        e.removeQuantity(qty);
        return e;
    }

    public List<Equipment> listAll() { return repo.findAll(); }
    public List<Equipment> search(String kw) { return repo.searchByName(kw); }
    public Optional<Equipment> find(int id) { return repo.findById(id); }
}

class BorrowService {
    private final EquipmentService equipmentService;
    private final List<Tx> txs = new ArrayList<>();

    public BorrowService(EquipmentService service){ this.equipmentService = service; }

    // กติกา: ถ้านิสิตยังมี "รายการยืมค้าง" ใดๆ อยู่ จะ "ไม่ให้ยืม" ชิ้นต่อไป
    private boolean hasAnyOpenOf(String studentId){
        for (Tx t : txs){
            if (t.isOpen() && t.getStudentId().equalsIgnoreCase(studentId)) return true;
        }
        return false;
    }

    // ยืม: ตรวจของ + available + ตรวจว่ามี OPEN อยู่หรือไม่
    public String borrow(String studentId, int itemId, int qty){
        // กฎสำคัญ: ห้ามยืมเพิ่มถ้ายังมีรายการ OPEN (ค้างคืน) อยู่
        if (hasAnyOpenOf(studentId)){
            return "ยืมไม่ได้: คุณยังมีอุปกรณ์ค้างคืนอยู่ กรุณาคืนให้ครบก่อนจึงจะยืมชิ้นใหม่ได้";
        }

        var e = equipmentService.find(itemId).orElse(null);
        if (e == null) return "ไม่พบอุปกรณ์";
        if (!e.borrow(qty)) return "ยืมไม่ได้ (คงเหลือไม่พอ)";
        txs.add(new Tx(studentId, itemId, e.getName(), qty, LocalDateTime.now()));
        return "ยืมสำเร็จ";
    }

    // คืน: เช็คยอดค้างของ student+item ก่อน, ถ้าคืนเกิน -> "ตัดรับ" เท่าที่ค้างจริง
    public String giveBack(String studentId, int itemId, int qty){
        var e = equipmentService.find(itemId).orElse(null);
        if (e == null) return "ไม่พบอุปกรณ์";

        int outstanding = outstandingQty(studentId, itemId); // ยอดค้างปัจจุบัน
        if (outstanding <= 0) {
            return "คืนไม่ได้: ไม่มีรายการยืมค้างของอุปกรณ์นี้ในชื่อ " + studentId;
        }

        int accept = Math.min(qty, outstanding); // cap ปริมาณคืนไม่เกินยอดค้าง
        try {
            e.giveBack(accept); // Validation ภายใน Equipment อีกชั้น
        } catch (Exception ex) {
            return "คืนไม่ได้: " + ex.getMessage();
        }

        // ปิดธุรกรรมตามจำนวนที่รับคืน (กลยุทธ์ LIFO + รองรับปิดบางส่วน)
        int toClose = accept;
        LocalDateTime now = LocalDateTime.now();
        for (int i = txs.size()-1; i>=0 && toClose>0; i--){
            Tx t = txs.get(i);
            if (!t.isOpen()) continue;
            if (t.getItemId()!=itemId) continue;
            if (!t.getStudentId().equals(studentId)) continue;

            if (t.getQty() <= toClose){
                toClose -= t.getQty();
                t.closeAll(now);  // ปิดทั้งบรรทัด
            } else {
                // ปิดบางส่วน: แตกบรรทัดเป็น closed + stillOpen เพื่อคงประวัติถูกต้อง
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

        if (qty > outstanding) {
            // แจ้งผู้ใช้ให้เข้าใจว่าระบบ "ตัดรับ" เท่าที่ค้างจริง
            return "คืนสำเร็จ (รับคืนเพียง " + accept + " จากที่ระบุ " + qty + " เพราะค้าง " + outstanding + ")";
        }
        return "คืนสำเร็จ";
    }

    // สรุปยอดยืมค้างสำหรับ student+item (ใช้ก่อนคืน)
    private int outstandingQty(String studentId, int itemId){
        int sum = 0;
        for (Tx t : txs){
            if (t.isOpen() && t.getItemId()==itemId && t.getStudentId().equalsIgnoreCase(studentId)){
                sum += t.getQty();
            }
        }
        return sum;
    }

    // สำหรับเมนูประวัติ
    public List<Tx> listAllTx(){ return txs; }
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
        equipmentService.addOrIncrease("ลูกฟุตบอล", 30);
        equipmentService.addOrIncrease("ลูกบาสเกตบอล", 20);
        equipmentService.addOrIncrease("ลูกวอลเลย์บอล", 25);
        equipmentService.addOrIncrease("ลูกแบดมินตัน", 100);
        equipmentService.addOrIncrease("ลูกเซปักตะกร้อ", 15);
        equipmentService.addOrIncrease("ไม้แบดมินตัน", 20);
        equipmentService.addOrIncrease("ไม้ปิงปอง", 20);
    }

    private void run(){
        seed();
        while(true){
            printMenu();
            System.out.print("เลือกเมนู: ");
            String c = sc.nextLine().trim();
            switch (c){
                case "1", "รายการทั้งหมด" -> doList(); 
                case "2", "ค้นหารายการ" -> doSearch(); 
                case "3", "เพิ่มรายการ" -> doAdd();
                case "4", "ลดจำนวน" -> doReduce();
                case "5", "ยืม อุปกรณ์" -> doBorrow();
                case "6", "คืน อุปกรณ์" -> doReturn();
                case "7", "ดูประวัติการยืม-คืน" -> showTx();
                case "8", "จบโปรแกรม" -> { System.out.println("จบโปรแกรม!!!"); return; }
                default -> System.out.println("เมนูไม่ถูกต้อง");
            }
            System.out.println();
        }
    }

    private void printMenu(){
        System.out.println("=== Equipment Menu ===");
        System.out.println("1) รายการทั้งหมด");
        System.out.println("2) ค้นหารายการ");
        System.out.println("3) เพิ่มรายการ");
        System.out.println("4) ลดจำนวน");
        System.out.println("5) ยืม อุปกรณ์");
        System.out.println("6) คืน อุปกรณ์");
        System.out.println("7) ดูประวัติการยืม-คืน");
        System.out.println("8) จบโปรแกรม");
    }

    // ---------- helpers สำหรับตารางสรุปรายการ ----------
    private static String pad(String s, int w){
        if (s == null) s = "";
        if (s.length() >= w) return s.substring(0, w);
        return s + " ".repeat(w - s.length());
    }
    private static String num(int n, int w){
        String s = String.valueOf(n);
        return " ".repeat(Math.max(0, w - s.length())) + s; // จัดชิดขวา
    }
    private void printEquipSummaryTable(List<Equipment> list){
        if (list == null || list.isEmpty()){
            System.out.println("ยังไม่มีรายการ");
            return;
        }
        list.sort(Comparator.comparingInt(Equipment::getId)); // เรียงตามรหัสให้อ่านง่าย
        System.out.println("--------------------------------------------------------------------------------");
        System.out.println(
            pad("รหัสอุปกรณ์ ", 15) + " | " +
            pad("ชื่อรายการ ", 20) + " | " +
            pad("ทั้งหมด ", 10) + "  | " +
            pad("คงเหลือ ", 7)
        );
        System.out.println("--------------------------------------------------------------------------------");
        for (Equipment e : list){
            System.out.println(
                pad("#" + e.getId(), 12) + " | " +
                pad(e.getName(), 20)     + " | " +
                num(e.getTotal(), 10)     + " | " +
                num(e.getAvailable(), 7)
            );
        }
        System.out.println("--------------------------------------------------------------------------------");
    }
    // ---------- end helpers ----------

    private void doAdd(){
        try{
            System.out.print("ชื่ออุปกรณ์: ");
            String name = sc.nextLine().trim();
            System.out.print("จำนวนที่เพิ่ม: ");
            int qty = Integer.parseInt(sc.nextLine().trim());

            // เรียก Service เพื่อบังคับกติกา "ชื่อซ้ำ -> เพิ่มจำนวน" (ไม่สร้าง object ซ้ำ)
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

    private void doReduce(){
        try{
            System.out.print("รหัส (#id): ");
            int id = Integer.parseInt(sc.nextLine().trim());
            System.out.print("จำนวนที่ลด: ");
            int qty = Integer.parseInt(sc.nextLine().trim());

            // ลดจำนวนผ่าน Service (ภายในตรวจห้ามต่ำกว่ายืมค้าง)
            Equipment e = equipmentService.reduceQuantity(id, qty);
            System.out.println("ลดจำนวนเรียบร้อย");
            System.out.println("สถานะปัจจุบัน: " + e);
        }catch(NumberFormatException nfe){
            System.out.println("กรอกตัวเลขให้ถูกต้อง");
        }catch(NoSuchElementException nf){
            System.out.println(nf.getMessage());
        }catch(IllegalArgumentException | IllegalStateException ex){
            System.out.println("ลดไม่ได้: " + ex.getMessage());
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
        printEquipSummaryTable(all); //  แสดงตาราง 4 คอลัมน์: รหัส | ชื่อ | ทั้งหมด | คงเหลือ
    }

    private void doBorrow(){
        try{
            System.out.print("รหัสนิสิต: ");
            String sid = sc.nextLine().trim();
            System.out.print("รหัสอุปกรณ์: ");
            int id = Integer.parseInt(sc.nextLine().trim());
            System.out.print("จำนวน: ");
            int qty = Integer.parseInt(sc.nextLine().trim());

            // เรียก BorrowService เพื่อยืม (เช็ค available และกติกา "มี OPEN ห้ามยืม")
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

            // คืน: มีการคำนวณยอดค้าง + ตัดรับคืนเท่าที่ค้างจริง + ปิดธุรกรรมแบบ LIFO
            String msg = borrowService.giveBack(sid, id, qty);
            System.out.println(msg);
        }catch(NumberFormatException nfe){
            System.out.println("กรอกตัวเลขให้ถูกต้อง");
        }
    }

    private void showTx(){
        // ตัวเลือก: ทั้งหมด / เฉพาะรหัสนิสิต
        System.out.println("ดูประวัติ: 1) all  2) by student");
        String m = sc.nextLine().trim();
        List<Tx> list = switch (m){
            case "2" -> {
                System.out.print("รหัสนิสิต: ");
                yield borrowService.listByStudent(sc.nextLine().trim());
            }
            default  -> borrowService.listAllTx();
        };
        if (list.isEmpty()){ System.out.println("ไม่มีประวัติ"); return; }
        System.out.println("รหัสนิสิต | รหัสอุปกรณ์  ชื่ออุปกรณ์      | จำนวน | วันและเวลาที่ยืม          | วันและเวลาที่คืน          | สถานะ");
        System.out.println("-------------------------------------------------------------------------------------------------");
        for (Tx t: list) System.out.println(t);
    }
}