package com.busbooking.test;

import com.busbooking.config.DBConnection;
import com.busbooking.dao.GenericDao;
import com.microsoft.sqlserver.jdbc.SQLServerDataSource;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * DaoTest - File chay thu thu cong bang ham main(), khong can Spring Boot chay.
 * Chay bang click phai -> Run 'main()' trong IDE.
 */
public class DaoTest {

    public static void main(String[] args) {

        // Cau hinh DataSource thu cong cho moi truong test
        SQLServerDataSource ds = new SQLServerDataSource();
        ds.setURL("jdbc:sqlserver://MSI\\MSSQLSERVER;databaseName=QuanLyVeXeKhach;instanceName=MSSQLSERVER;encrypt=true;trustServerCertificate=true;");
        ds.setUser("busapp");
        ds.setPassword("10101010aA@");

        GenericDao customerDao = new GenericDao(ds, "Customers");

        try {
            // 1. READ: Doc danh sach khach hang
            System.out.println("=== DANH SACH KHACH HANG ===");
            customerDao.findAll().forEach(System.out::println);

            // 2. CREATE: Them khach hang moi
            Map<String, Object> customer = new LinkedHashMap<>();
            customer.put("FullName", "Khach Hang Kiem Thu DAO");
            customer.put("Phone", "0987654321");
            customer.put("Email", "test.dao@example.com");
            customer.put("IdentityNumber", "TESTDAO001");
            customer.put("Address", "TP. Ho Chi Minh");

            Object newId = customerDao.insert(customer);

            if (newId == null) {
                System.out.println("Da INSERT; driver khong tra ve khoa moi.");
                return;
            }

            System.out.println("ID vua them: " + newId);

            // 3. READ BY ID
            System.out.println(customerDao.findById(newId));

            // 4. UPDATE
            Map<String, Object> changes = new LinkedHashMap<>();
            changes.put("FullName", "Khach Hang Da Cap Nhat");
            System.out.println("UPDATE: " + customerDao.update(newId, changes));

            // 5. DELETE
            System.out.println("DELETE: " + customerDao.delete(newId));

            // 6. Xac nhan da xoa
            System.out.println("Con ton tai? " + customerDao.findById(newId).isPresent());

        } catch (Exception e) {
            System.err.println("CRUD THAT BAI: " + e.getMessage());
            e.printStackTrace();
        }
    }
}