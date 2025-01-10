package com.limelight.zerotier.db;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;


import com.limelight.zerotier.model.AppNode;
import com.limelight.zerotier.model.AssignedAddress;
import com.limelight.zerotier.model.DnsServer;
import com.limelight.zerotier.model.Network;
import com.limelight.zerotier.model.NetworkConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ZTDatabase {
    private static final String DATABASE_NAME = "ztfixdb";
    private static final int DATABASE_VERSION = 22;

    private final SQLiteDatabase db;
    private static ZTDatabase instance;

    // 读写锁
    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    public final Lock readLock = readWriteLock.readLock();
    public final Lock writeLock = readWriteLock.writeLock();

    private ZTDatabase(Context context) {
        SQLiteOpenHelper helper = new SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
            @Override
            public void onCreate(SQLiteDatabase db) {
                createTables(db);
            }

            @Override
            public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
                if (oldVersion < 19) {
                    db.execSQL("ALTER TABLE network_config ADD COLUMN dnsMode INTEGER NOT NULL DEFAULT 0");
                }
                if (oldVersion < 20) {
                    db.execSQL("UPDATE network_config SET dnsMode = 2 WHERE useCustomDNS = 1");
                }
            }

            private void createTables(SQLiteDatabase db) {
                db.execSQL("CREATE TABLE network (" +
                        "networkId INTEGER PRIMARY KEY," +
                        "networkIdStr TEXT," +
                        "networkName TEXT," +
                        "lastActivated INTEGER," +
                        "networkConfigId INTEGER" +
                        ")");

                db.execSQL("CREATE TABLE network_config (" +
                        "id INTEGER PRIMARY KEY," +
                        "routeViaZeroTier INTEGER," +
                        "dnsMode INTEGER," +
                        "useCustomDNS INTEGER," +
                        "type INTEGER," +
                        "status INTEGER," +
                        "mac TEXT," +
                        "mtu TEXT," +
                        "broadcast INTEGER," +
                        "bridging INTEGER" +
                        ")");

                db.execSQL("CREATE TABLE assigned_address (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "networkId INTEGER," +
                        "addressString TEXT," +
                        "prefix INTEGER" +
                        ")");

                db.execSQL("CREATE TABLE dns_server (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "networkId INTEGER," +
                        "nameserver TEXT" +
                        ")");

                db.execSQL("CREATE TABLE app_node (" +
                        "nodeId INTEGER PRIMARY KEY," +
                        "nodeIdStr TEXT" +
                        ")");

                db.execSQL("CREATE TABLE moon_orbit (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "moonWorldId INTEGER," +
                        "moonSeed INTEGER," +
                        "fromFile INTEGER NOT NULL DEFAULT 0" +
                        ")");
            }
        };
        db = helper.getWritableDatabase();
    }

    public static synchronized ZTDatabase getInstance(Context context) {
        if (instance == null) {
            instance = new ZTDatabase(context.getApplicationContext());
        }
        return instance;
    }

    // Network相关操作
    public Network getNetworkById(long networkId) {
        readLock.lock();
        try {
            Cursor cursor = db.query("network", null,
                    "networkId = ?",
                    new String[]{String.valueOf(networkId)},
                    null, null, null);

            Network network = null;
            if (cursor.moveToFirst()) {
                network = new Network();
                network.setNetworkId(cursor.getLong(cursor.getColumnIndex("networkId")));
                network.setNetworkName(cursor.getString(cursor.getColumnIndex("networkName")));
                network.setLastActivated(cursor.getInt(cursor.getColumnIndex("lastActivated")) == 1);
                network.setNetworkConfigId(cursor.getLong(cursor.getColumnIndex("networkConfigId")));
            }
            cursor.close();
            return network;
        } finally {
            readLock.unlock();
        }
    }

    // AppNode相关操作
    public void saveAppNode(AppNode node) {
        writeLock.lock();
        try {
            ContentValues values = new ContentValues();
            values.put("nodeId", node.getNodeId());
            values.put("nodeIdStr", node.getNodeIdStr());
            db.insertWithOnConflict("app_node", null, values, SQLiteDatabase.CONFLICT_REPLACE);
        } finally {
            writeLock.unlock();
        }
    }

    public List<AppNode> getAllAppNodes() {
        readLock.lock();
        try {
            List<AppNode> nodes = new ArrayList<>();
            Cursor cursor = db.query("app_node", null,
                    null, null, null, null, null);

            while (cursor.moveToNext()) {
                AppNode node = new AppNode();
                node.setNodeId(cursor.getLong(cursor.getColumnIndex("nodeId")));
                node.setNodeIdStr(cursor.getString(cursor.getColumnIndex("nodeIdStr")));
                nodes.add(node);
            }
            cursor.close();
            return nodes;
        } finally {
            readLock.unlock();
        }
    }

    public List<Network> getLastActivatedNetworks() {
        readLock.lock();
        try {
            List<Network> networks = new ArrayList<>();
            Cursor cursor = db.query("network", null,
                    "lastActivated = 1",
                    null, null, null, null);

            while (cursor.moveToNext()) {
                Network network = new Network();
                network.setNetworkId(cursor.getLong(cursor.getColumnIndex("networkId")));
                network.setNetworkName(cursor.getString(cursor.getColumnIndex("networkName")));
                network.setLastActivated(true);
                network.setNetworkConfigId(cursor.getLong(cursor.getColumnIndex("networkConfigId")));
                networks.add(network);
            }
            cursor.close();
            return networks;
        } finally {
            readLock.unlock();
        }
    }

    public void saveNetwork(Network network) {
        writeLock.lock();
        try {
            ContentValues values = new ContentValues();
            values.put("networkId", network.getNetworkId());
            values.put("networkName", network.getNetworkName());
            values.put("lastActivated", network.getLastActivated() ? 1 : 0);
            values.put("networkConfigId", network.getNetworkConfigId());

            db.insertWithOnConflict("network", null, values, SQLiteDatabase.CONFLICT_REPLACE);
        } finally {
            writeLock.unlock();
        }
    }

    public void deleteNetwork(Network network) {
        writeLock.lock();
        try {
            db.delete("network", "networkId = ?",
                    new String[]{String.valueOf(network.getNetworkId())});
        } finally {
            writeLock.unlock();
        }
    }

    public void saveNetworkConfig(NetworkConfig config) {
        writeLock.lock();
        try {
            ContentValues values = new ContentValues();
            values.put("id", config.getId());
            values.put("routeViaZeroTier", config.getRouteViaZeroTier() ? 1 : 0);
            values.put("dnsMode", config.getDnsMode());
            NetworkConfig.NetworkTypeConverter typeConverter = new NetworkConfig.NetworkTypeConverter();
            NetworkConfig.NetworkStatusConverter statusConverter = new NetworkConfig.NetworkStatusConverter();
            values.put("type", typeConverter.convertToDatabaseValue(config.getType()));
            values.put("status", statusConverter.convertToDatabaseValue(config.getStatus()));
            values.put("mac", config.getMac());
            values.put("mtu", config.getMtu());
            values.put("broadcast", config.getBroadcast() ? 1 : 0);
            values.put("bridging", config.getBridging() ? 1 : 0);

            db.insertWithOnConflict("network_config", null, values, SQLiteDatabase.CONFLICT_REPLACE);
        } finally {
            writeLock.unlock();
        }
    }

    public void deleteNetworkConfig(NetworkConfig config) {
        writeLock.lock();
        try {
            db.delete("network_config", "id = ?",
                    new String[]{String.valueOf(config.getId())});
        } finally {
            writeLock.unlock();
        }
    }

    public NetworkConfig getNetworkConfig(long id) {
        readLock.lock();
        try {
            Cursor cursor = db.query("network_config", null,
                    "id = ?",
                    new String[]{String.valueOf(id)},
                    null, null, null);

            NetworkConfig config = null;
            if (cursor.moveToFirst()) {
                config = new NetworkConfig();
                config.setId(cursor.getLong(cursor.getColumnIndex("id")));
                config.setRouteViaZeroTier(cursor.getInt(cursor.getColumnIndex("routeViaZeroTier")) == 1);
                config.setDnsMode(cursor.getInt(cursor.getColumnIndex("dnsMode")));
                NetworkConfig.NetworkTypeConverter typeConverter = new NetworkConfig.NetworkTypeConverter();
                NetworkConfig.NetworkStatusConverter statusConverter = new NetworkConfig.NetworkStatusConverter();
                config.setType(typeConverter.convertToEntityProperty(cursor.getInt(cursor.getColumnIndex("type"))));
                config.setStatus(statusConverter.convertToEntityProperty(cursor.getInt(cursor.getColumnIndex("status"))));
                config.setMac(cursor.getString(cursor.getColumnIndex("mac")));
                config.setMtu(cursor.getString(cursor.getColumnIndex("mtu")));
                config.setBroadcast(cursor.getInt(cursor.getColumnIndex("broadcast")) == 1);
                config.setBridging(cursor.getInt(cursor.getColumnIndex("bridging")) == 1);
            }
            cursor.close();
            return config;
        } finally {
            readLock.unlock();
        }
    }

    public List<DnsServer> getDnsServers(long networkConfigId) {
        readLock.lock();
        try {
            List<DnsServer> servers = new ArrayList<>();
            Cursor cursor = db.query("dns_server", null,
                    "networkId = ?",
                    new String[]{String.valueOf(networkConfigId)},
                    null, null, null);

            while (cursor.moveToNext()) {
                DnsServer server = new DnsServer();
                server.setId(cursor.getLong(cursor.getColumnIndex("id")));
                server.setNetworkId(networkConfigId);
                server.setNameserver(cursor.getString(cursor.getColumnIndex("nameserver")));
                servers.add(server);
            }
            cursor.close();
            return servers;
        } finally {
            readLock.unlock();
        }
    }

    public void saveDnsServers(long networkConfigId, List<DnsServer> servers) {
        writeLock.lock();
        try {
            // 先删除旧的
            db.delete("dns_server", "networkId = ?",
                    new String[]{String.valueOf(networkConfigId)});

            // 保存新的
            for (DnsServer server : servers) {
                ContentValues values = new ContentValues();
                values.put("networkId", networkConfigId);
                values.put("nameserver", server.getNameserver());
                db.insertWithOnConflict("dns_server", null, values, SQLiteDatabase.CONFLICT_REPLACE);
            }
        } finally {
            writeLock.unlock();
        }
    }

    public List<AssignedAddress> getAssignedAddresses(long networkConfigId) {
        readLock.lock();
        try {
            List<AssignedAddress> addresses = new ArrayList<>();
            Cursor cursor = db.query("assigned_address", null,
                    "networkId = ?",
                    new String[]{String.valueOf(networkConfigId)},
                    null, null, null);

            while (cursor.moveToNext()) {
                AssignedAddress address = new AssignedAddress();
                address.setId(cursor.getLong(cursor.getColumnIndex("id")));
                address.setNetworkId(networkConfigId);
                address.setAddressString(cursor.getString(cursor.getColumnIndex("addressString")));
                address.setPrefix(cursor.getInt(cursor.getColumnIndex("prefix")));
                addresses.add(address);
            }
            cursor.close();
            return addresses;
        } finally {
            readLock.unlock();
        }
    }

    public void saveAssignedAddresses(long networkConfigId, List<AssignedAddress> addresses) {
        writeLock.lock();
        try {
            // 先删除旧的
            db.delete("assigned_address", "networkId = ?",
                    new String[]{String.valueOf(networkConfigId)});

            // 保存新的
            for (AssignedAddress address : addresses) {
                ContentValues values = new ContentValues();
                values.put("networkId", networkConfigId);
                values.put("addressString", address.getAddressString());
                values.put("prefix", address.getPrefix());
                db.insertWithOnConflict("assigned_address", null, values, SQLiteDatabase.CONFLICT_REPLACE);
            }
        } finally {
            writeLock.unlock();
        }
    }
}