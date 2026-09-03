/* calcula-web — frontend de la plataforma.
 *
 * OJO: aqui NO hay logica de negocio. Las reglas de planilla viven solo
 * en Java (PlanillaService), que es donde JUnit y JaCoCo las cubren.
 * Duplicarlas en el cliente seria un defecto, no una optimizacion.
 */
(function () {
  "use strict";

  var $ = function (id) { return document.getElementById(id); };
  var API = "";   // mismo origen: Nginx hace de proxy hacia la API

  var fmt = function (n) {
    return Number(n).toLocaleString("es-BO", {
      minimumFractionDigits: 2, maximumFractionDigits: 2
    });
  };

  function fila(k, sub, v, cls) {
    return '<div class="row"><div class="k">' + k +
           (sub ? '<small>' + sub + '</small>' : '') +
           '</div><div class="v' + (cls ? ' ' + cls : '') + '">' + v + '</div></div>';
  }

  // ---------- identidad de la instancia ----------
  function refrescarInstancia() {
    fetch(API + "/api/instance")
      .then(function (r) { return r.json(); })
      .then(function (d) {
        var s = $("served");
        s.className = "served " + (d.instance === "GREEN" ? "g" : "b");
        $("served-who").textContent = d.instance;
        $("served-ver").textContent = "v" + d.version + " · :" + d.port;
      })
      .catch(function () {
        $("served").className = "served err";
        $("served-who").textContent = "sin respuesta";
        $("served-ver").textContent = "";
      });
  }

  // ---------- liquidacion ----------
  function calcular() {
    var salario = $("salario").value;
    var anios = $("anios").value;
    var out = $("salida");

    fetch(API + "/api/planilla/liquidar?salario=" + encodeURIComponent(salario) +
                "&anios=" + encodeURIComponent(anios))
      .then(function (r) {
        return r.json().then(function (d) { return { ok: r.ok, d: d }; });
      })
      .then(function (res) {
        if (!res.ok) {
          out.innerHTML = '<div class="err">' + (res.d.error || "Entrada invalida") + '</div>';
          return;
        }
        var d = res.d;
        var html = '<div class="breakdown">';
        html += fila("Salario bruto", null, fmt(d.salarioBruto));
        html += fila("Bono de antigüedad",
                     (d.porcentajeAntiguedad * 100).toFixed(0) + " % del mínimo · " + anios + " años",
                     fmt(d.bonoAntiguedad));
        html += fila("Total ganado", null, fmt(d.totalGanado));
        html += fila("Aporte AFP", d.topeAfpAplicado ? "tope aplicado" : null,
                     "−" + fmt(d.aporteAfp), "neg");
        html += fila("RC-IVA", d.exentoRcIva ? "exento" : "sobre el excedente",
                     "−" + fmt(d.rcIva), "neg");
        html += '<div class="row sum"><div class="k">Líquido pagable</div>' +
                '<div class="v">Bs ' + fmt(d.liquidoPagable) + '</div></div></div>';
        out.innerHTML = html;
        refrescarInstancia();
      })
      .catch(function () {
        out.innerHTML = '<div class="err">No se pudo contactar con la API.</div>';
      });
  }

  // ---------- prueba de trafico ----------
  function traffic() {
    var dots = $("dots");
    dots.innerHTML = "";
    var cb = 0, cg = 0, ce = 0;
    $("t-b").textContent = "0"; $("t-g").textContent = "0"; $("t-e").textContent = "0";
    $("btn-traffic").disabled = true;

    var i = 0;
    (function paso() {
      if (i >= 20) { $("btn-traffic").disabled = false; refrescarInstancia(); return; }
      i++;
      fetch(API + "/api/instance")
        .then(function (r) { return r.json(); })
        .then(function (d) {
          var cls = d.instance === "GREEN" ? "g" : "b";
          if (cls === "g") { cg++; $("t-g").textContent = cg; }
          else             { cb++; $("t-b").textContent = cb; }
          return cls;
        })
        .catch(function () { ce++; $("t-e").textContent = ce; return "e"; })
        .then(function (cls) {
          var el = document.createElement("i");
          el.className = cls;
          dots.appendChild(el);
          setTimeout(paso, 60);
        });
    })();
  }

  $("btn-calc").addEventListener("click", calcular);
  $("btn-traffic").addEventListener("click", traffic);
  ["salario", "anios"].forEach(function (id) {
    $(id).addEventListener("keydown", function (e) { if (e.key === "Enter") { calcular(); } });
  });

  refrescarInstancia();
  calcular();
})();
