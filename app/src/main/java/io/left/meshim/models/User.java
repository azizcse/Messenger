package io.left.meshim.models;

import static android.content.Context.MODE_PRIVATE;
import static io.left.meshim.BuildConfig.APPLICATION_ID;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.Index;
import android.arch.persistence.room.PrimaryKey;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import io.left.meshim.controllers.RightMeshController;
import io.left.rightmesh.id.MeshID;

import java.lang.reflect.Type;


/**
 * This class represents a user of the app, both the user on the device the code is running on and
 * other users on the mesh. It is decorated as a Room entity, and Parcelable for passing back and
 * forth between services.
 *
 * <p>
 *     Also of note is that one instance of this class is stored in SharedPreferences, representing
 *     the user of this device.
 * </p>
 */
@Entity(tableName = "Users",
        indices = {@Index(value = {"UserID", "MeshID"}, unique = true)})
public class User implements Parcelable {
    // Used in shared preference to save or load data.
    @Ignore
    private static final String SAVE_VERSION = "UserDataSaveVersion_v1";

    // ID of the device's user, achieved by ensuring it is the first one inserted into the database.
    @Ignore
    public static final int DEVICE_USER_ID = 1;

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "UserID")
    public int id;

    @ColumnInfo(name = "MeshID")
    private MeshID meshId = new MeshID();

    @ColumnInfo(name = "Username")
    private String username;

    @ColumnInfo(name = "Avatar")
    private int avatar;

    // SharedPreferences is a singleton - the same reference is always returned. It also updates
    // itself in a threadsafe way, so might as well keep one version of it open.
    // The transient qualifier makes Gson ignore it for serialization.
    @Ignore
    private transient SharedPreferences preferences;

    public User() {
        this("Anonymous", -1);
    }

    /**
     * Constructor with {@link MeshID} option. Used in
     * {@link RightMeshController} where we care about mesh logic.
     * @param username displayed username for the user
     * @param avatar avatar chosen by the user
     * @param meshId ID of the user on the mesh
     */
    @Ignore
    public User(String username, int avatar, MeshID meshId) {
        this(username, avatar);
        this.meshId = meshId;
    }

    /**
     * Constructor with only username and avatar. Used in the UI layer where we don't care about
     * mesh logic.
     * @param username displayed username for the user
     * @param avatar avatar chosen by the user
     */
    @Ignore
    public User(String username, int avatar) {
        this.avatar = avatar;
        this.username = username;
    }

    /**
     * Creating a user with a Context class means it can store a link to SharedPreferences, meaning
     * {@link User#load()} and {@link User#save()} work.
     *
     * @param context to load SharedPreferences from
     */
    @Ignore
    public User(Context context) {
        this();
        preferences = context.getSharedPreferences(APPLICATION_ID, MODE_PRIVATE);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof User && ((User) o).id == this.id;
    }

    @Override
    public int hashCode() {
        return this.id;
    }

    /**
     * Attempts to load the stored {@link User} from {@link SharedPreferences}.
     *
     * @param context to load {@link SharedPreferences} from.
     * @return instance loaded from disk, or null
     */
    public static User fromDisk(Context context) {
        User temp = new User(context);
        if (!temp.load()) {
            return null;
        }
        return temp;
    }

    public MeshID getMeshId() {
        return meshId;
    }

    public void setMeshId(MeshID meshId) {
        this.meshId = meshId;
    }

    public int getAvatar() {

        return avatar;
    }

    public String getUsername() {
        return username;
    }

    public void setAvatar(int avatar) {
        this.avatar = avatar;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * A constructor for the parcel data type.
     *
     * <p>
     *     Extracts username and uyserAvatar from parcel data type
     * </p>
     * @param in parel to parse
     */
    private User(Parcel in) {
        this.id = in.readInt();
        this.username = in.readString();
        this.avatar = in.readInt();
        byte[] uuid = new byte[20];
        in.readByteArray(uuid);
        this.meshId = new MeshID(uuid);
    }


    public static final Creator<User> CREATOR = new Creator<User>() {
        @Override
        public User createFromParcel(Parcel in) {
            return new User(in);
        }

        @Override
        public User[] newArray(int size) {
            return new User[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * flatten the object in parcel.
     * @param dest needed by Parcelable
     * @param flags needed by Parcelable
     */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.id);
        dest.writeString(this.getUsername());
        dest.writeInt(this.avatar);
        dest.writeByteArray(this.meshId.getRawUuid());
    }

    /**
     * This function loads data from SharedPreferences if it exists.
     * @return true if function was able to load else false
     */
    public boolean load() {
        try {
            Gson gson = new Gson();
            String user = preferences.getString(SAVE_VERSION, null);
            Type type = new TypeToken<User>() {
            }.getType();
            User temp = gson.fromJson(user, type);
            if (temp == null) {
                return false;
            } else {
                this.id = DEVICE_USER_ID;
                this.setAvatar(temp.getAvatar());
                this.setUsername(temp.getUsername());
            }
            return true;
        } catch (NullPointerException npe) {
            // If preferences is null, we can't load anything.
            return false;
        }
    }

    /**
     * This function saves data to SharedPreferences.
     */
    public void save() {
        try {
            SharedPreferences.Editor editor = preferences.edit();
            Gson gsonModel = new Gson();
            String savemodel = gsonModel.toJson(this);
            editor.putString(SAVE_VERSION, savemodel);
            editor.commit();
        } catch (NullPointerException ignored) {
            // In case preferences is null.
        }
    }
}
