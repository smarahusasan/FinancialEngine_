# Financial Trading Execution Engine

## 1. Descriere generală
Acest proiect implementează un Sistem de Execuție a Ordinelor de Tranzacționare (Financial Engine) care simulează funcționarea unei platforme de tranzacționare automatizate pentru instrumente financiare (acțiuni și criptomonede).

Sistemul primește ordine de tip BUY Limit și SELL Limit, gestionează lichiditatea disponibilă pentru fiecare instrument, execută ordinele conform condițiilor de piață sau le anulează la expirare și notifică asincron clienții asupra rezultatului.

Accentul principal este pus pe:
- concurență și sincronizare
- viteză și consistență a alocării resurselor
- audit periodic și control al riscului

---

## 2. Funcționalități implementate
- Ordine BUY / SELL Limit
- Pre-alocare volum (lichiditate garantată)
- Execuție automată pe baza prețului de piață
- Anulare automată la expirarea ordinului
- Actualizare dinamică a prețului instrumentelor (model stochastic)
- Audit periodic al sistemului
- Calcul profit și comision
- Notificare asincronă a clienților folosind CompletableFuture
- Procesare concurentă folosind thread pool
- Simulare roboți de trading
- Persistență în fișiere text
- Shutdown automat al serverului

---

## 3. Tipuri de ordine
Sistemul suportă două tipuri de ordine:
- BUY Limit – ordin de cumpărare la un preț maxim
- SELL Limit – ordin de vânzare la un preț minim

Format ordin:
clientId,instrument,BUY|SELL,volume,limitPrice

Exemplu:
1,AAPL,BUY,10,105.50

---

## 4. Modelul de preț al instrumentelor
Prețul fiecărui instrument financiar este actualizat periodic folosind modelul:

newPrice = prevPrice + μ * dt + σ * sqrt(dt) * ε

unde:
- μ – trendul prețului
- σ – volatilitatea
- dt – interval de timp
- ε – variabilă aleatoare cu distribuție normală N(0,1)

---

## 5. Fluxul de execuție al unui ordin

### Pasul 1 – Pre-alocare
1. Clientul trimite ordinul către server
2. Sistemul verifică lichiditatea disponibilă
3. Dacă volumul este disponibil:
    - volumul este alocat temporar
    - ordinul primește status PENDING
    - ordinul este adăugat în OrderBook
4. Dacă volumul nu este disponibil:
    - ordinul este respins (REJECTED)

### Pasul 2 – Execuție / Anulare
Un proces de audit rulează periodic și:
- execută ordinele care respectă condițiile de piață
- anulează ordinele care au depășit timpul de expirare

Statusuri:
- PENDING
- EXECUTED
- CANCELLED
- REJECTED

---

## 6. Concurență și sincronizare
- ExecutorService cu număr fix de thread-uri
- AtomicInteger pentru gestionarea lichidității
- synchronized pentru actualizarea statusului ordinelor
- audit și execuție în thread-uri separate
- fără blocaje globale

---

## 7. Futures și notificarea clienților
Pentru fiecare ordin se utilizează un CompletableFuture:
- clientul este notificat automat la execuție sau anulare
- comunicarea este asincronă
- serverul nu blochează thread-uri

---

## 8. Audit periodic și controlul riscului
Auditul rulează la fiecare 2 secunde și:
- verifică ordinele PENDING
- execută sau anulează ordine
- validează lichiditatea instrumentelor
- calculează profitul per instrument
- salvează starea în fișiere text

Comision: 0.5% din valoarea tranzacției.

---

## 9. Persistență
Datele sunt salvate în fișiere text:

### Registre de Persistență:

#### 1. Registrul Ordinelor (order_registry.txt)
Format: `id_ordin, id_client, instrument, tip_ordin, volum_solicitat, preț, status, oră_plasare`
- Înregistrează toate ordinele noi cu status PENDING
- Înregistrează actualizările de status (EXECUTED, CANCELLED, REJECTED)

#### 2. Registrul Execuțiilor (execution_registry.txt)
Format: `id_execuție, id_ordin, data, volum_executat, preț_final, comision`
- Înregistrează toate execuțiile de ordine
- Comision: 0.5% din prețul total al tranzacției

#### 3. Registrul Anulărilor (cancellation_registry.txt)
Format: `id_anulare, id_ordin, data, motiv_anulare`
- Înregistrează toate anulările de ordine
- Include motivul anulării (de exemplu: expirare după 30 secunde)

### 4. Audit
La fiecare execuție a procesului de audit, se salvează
pentru fiecare instrument prețul, profitul și lichiditatea
disponibila. Se salvează și ordinele PENDING.

Fișiere principale:
- order_registry.txt (Registrul Ordinelor)
- execution_registry.txt (Registrul Execuțiilor)
- cancellation_registry.txt (Registrul Anulărilor)
- audit.log 

---

## 10. Simulare clienți
- 5 roboți de trading
- fiecare trimite un ordin la fiecare 1 secundă
- parametrii sunt aleși aleator

---

## 11. Arhitectura proiectului
Structura proiectului:

model/     – entități de domeniu  
engine/    – logică de execuție și audit  
server/    – networking și procesare clienți  
client/    – simulare roboți  
persistence/      – salvare în fișier de info

---

## 12. Tehnologii utilizate
- Java 17+
- TCP Sockets
- ExecutorService / ScheduledExecutorService
- AtomicInteger
- CompletableFuture
- File I/O

---

## 13. Rulare aplicație

Compilare:
javac Main.java

Rulare:
java Main

---

## 14. Comportament la rulare
- Serverul pornește pe portul 5000
- Roboții trimit ordine automat
- Auditul rulează periodic
- Fișierele de audit sunt generate
- Serverul se închide automat după 3 minute

---

## 15. Parametri configurabili
- Număr thread-uri
- Timp expirare ordin
- Interval audit
- Volatilitate și trend preț
- Durata rulării serverului

---

## 16. Concluzie
Proiectul demonstrează un engine financiar concurent și scalabil, cu audit periodic și notificare asincronă a clienților, respectând principiile programării concurente și arhitecturii modulare.

