package com.monokek.identity.domain;

import com.monokek.common.Timestamps;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Port of {@code App\Models\Device} — a POS/kitchen/mobile terminal, tracked
 * so it can be identified/whitelisted independently of the user logged in on it.
 */
@Entity
@Table(name = "devices")
@Getter
@Setter
@NoArgsConstructor
public class Device extends Timestamps {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nullable: a device is not always tied to the currently logged-in user. */
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "device_name", nullable = false)
    private String deviceName;

    /** mobile, pos, kitchen */
    @Column(name = "device_type", nullable = false)
    private String deviceType;

    @Column(name = "device_uuid", nullable = false, unique = true)
    private String deviceUuid;

    @Column(name = "ip_address")
    private String ipAddress;
}
