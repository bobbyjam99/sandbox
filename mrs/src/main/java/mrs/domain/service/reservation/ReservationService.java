package mrs.domain.service.reservation;

import mrs.domain.model.*;
import mrs.domain.repository.reservation.ReservationRepository;
import mrs.domain.repository.room.ReservableRoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@Transactional
public class ReservationService {
    @Autowired
    ReservationRepository reservationRepository;
    @Autowired
    ReservableRoomRepository reservableRoomRepository;

    public List<Reservation> findReservations(ReservableRoomId reservableRoomId) {
        return reservationRepository.findByReservableRoom_ReservableRoomIdOrderByStartTimeAsc(reservableRoomId);
    }

    public Reservation reserve(Reservation reservation) {
        ReservableRoomId reservableRoomId = reservation.getReservableRoom().getReservableRoomId();
        // 対象の部屋が予約可能かどうかチェック
        ReservableRoom reservable = reservableRoomRepository.findOne(reservableRoomId);
        if (reservable == null) {
            throw new UnavailableReservationException("入力の日付・部屋の組み合わせは予約できません。");
        }
        // 重複チェック
        boolean overlap = reservationRepository.findByReservableRoom_ReservableRoomIdOrderByStartTimeAsc(reservableRoomId).stream().anyMatch(x -> x.overlap(reservation));

        if (overlap) {
            throw new AlreadyReservedException("入力の時間帯はすでに予約済みです。");
        }
        // 予約情報の登録
        reservationRepository.save(reservation);
        return reservation;
    }

    public void cancel(Integer reservationId, User requsetUser) {
        Reservation reservation = reservationRepository.findOne(reservationId);
        if (RoleName.ADMIN != requsetUser.getRoleName() &&
                !Objects.equals(reservation.getUser().getUserId(), requsetUser.getUserId())) {
            throw new IllegalStateException("要求されたキャンセルは許可できません。");
        }
        reservationRepository.delete(reservation);
    }
}
