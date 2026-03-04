package edu.RhPro.services;

import edu.RhPro.entities.Tache;
import edu.RhPro.entities.TacheV2;
import edu.RhPro.tools.MyConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TacheV2Service {

    private final TacheService baseService = new TacheService();
    private final Connection cnx = MyConnection.getInstance().getCnx();

    public List<TacheV2> findDoneByEmployeId(int employeId) throws Exception {
        List<Tache> base = baseService.findDoneByEmployeId(employeId);
        if (base == null || base.isEmpty()) return Collections.emptyList();

        List<Integer> ids = new ArrayList<>();
        for (Tache t : base) ids.add(t.getId());

        Map<Integer, Meta> meta = fetchMetaByIds(ids);

        List<TacheV2> out = new ArrayList<>();
        for (Tache t : base) {
            Meta m = meta.get(t.getId());
            LocalDate dd = m == null ? null : m.debut;
            LocalDate df = m == null ? null : m.fin;
            int level = m == null ? 1 : m.level;
            out.add(new TacheV2(t, dd, df, level));
        }
        return out;
    }

    private static class Meta {
        final LocalDate debut;
        final LocalDate fin;
        final int level;

        Meta(LocalDate debut, LocalDate fin, int level) {
            this.debut = debut;
            this.fin = fin;
            this.level = level;
        }
    }

    private Map<Integer, Meta> fetchMetaByIds(List<Integer> ids) throws Exception {
        Map<Integer, Meta> map = new HashMap<>();
        if (ids == null || ids.isEmpty()) return map;

        String placeholders = String.join(",", Collections.nCopies(ids.size(), "?"));
        String sql = "SELECT id, date_debut, date_fin, level FROM tache WHERE id IN (" + placeholders + ")";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            for (int i = 0; i < ids.size(); i++) ps.setInt(i + 1, ids.get(i));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Date dd = rs.getDate("date_debut");
                    Date df = rs.getDate("date_fin");
                    int level = rs.getInt("level");

                    map.put(
                            rs.getInt("id"),
                            new Meta(
                                    dd == null ? null : dd.toLocalDate(),
                                    df == null ? null : df.toLocalDate(),
                                    level
                            )
                    );
                }
            }
        }
        return map;
    }
}