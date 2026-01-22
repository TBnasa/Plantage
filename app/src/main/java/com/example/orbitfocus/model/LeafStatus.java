package com.example.orbitfocus.model;

/**
 * Yaprak durumlarını tanımlayan Enum.
 * Her yaprak bu üç durumdan birinde olabilir.
 */
public enum LeafStatus {
    /**
     * ACTIVE (Canlı):
     * - Bugün oluşturulmuş ve ertesi gün 00:00'a (gece yarısı) kadar
     * düzenlenebilir.
     * - Fotoğraf, yazı veya video eklenebilir.
     * - Renk: Canlı Yeşil
     */
    ACTIVE,

    /**
     * LOCKED (Ölümsüz/Kilitli):
     * - Kullanıcı süresi dolmadan içerik ekledi.
     * - Süre dolduktan sonra "Salt Okunur" (Read-Only).
     * - Kullanıcı anısını görebilir ama ASLA düzenleyemez veya silemez.
     * - Renk: Canlı yeşil tonu
     */
    LOCKED,

    /**
     * WITHERED (Kurumuş):
     * - Kullanıcı ertesi gün 00:00'a (gece yarısı) kadar hiçbir veri girmedi.
     * - Yaprak kahverengiye döner, kurur.
     * - O güne bir daha veri eklenemez.
     * - Tıklandığında "Bu günü kaçırdın" uyarısı gösterilir.
     * - Renk: Kahverengi
     */
    WITHERED
}
