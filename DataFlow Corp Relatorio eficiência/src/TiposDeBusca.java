import java.io.*;
import java.util.*;

public class CorridaDaEficiencia {
    static final int EXECUCOES = 4;
    static final int[] TAMANHOS = {100, 1000, 5000, 10000, 50000, 100000};
    static final Random ALEATORIO = new Random(42);

    static class Metricas {
        long comparacoes;
        long movimentos;
        long nanos;
        int indiceResultado;
        boolean correto;
    }

    static Metricas buscaLinear(int[] a, int alvo) {
        Metricas m = new Metricas();
        long t = System.nanoTime();
        for (int i = 0; i < a.length; i++) {
            m.comparacoes++;
            if (a[i] == alvo) { m.indiceResultado = i; m.nanos = System.nanoTime() - t; return m; }
        }
        m.indiceResultado = -1; m.nanos = System.nanoTime() - t; return m;
    }

    static Metricas buscaBinaria(int[] a, int alvo) {
        Metricas m = new Metricas();
        int lo = 0, hi = a.length - 1;
        long t = System.nanoTime();
        while (lo <= hi) {
            int mid = lo + (hi - lo) / 2;
            m.comparacoes++;
            if (a[mid] == alvo) { m.indiceResultado = mid; m.nanos = System.nanoTime() - t; return m; }
            m.comparacoes++;
            if (a[mid] < alvo) lo = mid + 1; else hi = mid - 1;
        }
        m.indiceResultado = -1; m.nanos = System.nanoTime() - t; return m;
    }

    static Metricas selectionSort(int[] a) {
        Metricas m = new Metricas();
        long t = System.nanoTime();
        for (int i = 0; i < a.length - 1; i++) {
            int min = i;
            for (int j = i + 1; j < a.length; j++) {
                m.comparacoes++;
                if (a[j] < a[min]) min = j;
            }
            if (min != i) {
                int tmp = a[i]; a[i] = a[min]; a[min] = tmp;
                m.movimentos += 3;
            }
        }
        m.nanos = System.nanoTime() - t;
        m.correto = estaOrdenado(a);
        return m;
    }

    static Metricas mergeSort(int[] a) {
        Metricas m = new Metricas();
        int[] aux = new int[a.length];
        long t = System.nanoTime();
        mergeSortRec(a, aux, 0, a.length - 1, m);
        m.nanos = System.nanoTime() - t;
        m.correto = estaOrdenado(a);
        return m;
    }

    static void mergeSortRec(int[] a, int[] aux, int lo, int hi, Metricas m) {
        if (lo >= hi) return;
        int mid = lo + (hi - lo) / 2;
        mergeSortRec(a, aux, lo, mid, m);
        mergeSortRec(a, aux, mid + 1, hi, m);
        int i = lo, j = mid + 1, k = lo;
        while (i <= mid && j <= hi) {
            m.comparacoes++;
            if (a[i] <= a[j]) aux[k++] = a[i++]; else aux[k++] = a[j++];
            m.movimentos++;
        }
        while (i <= mid) { aux[k++] = a[i++]; m.movimentos++; }
        while (j <= hi) { aux[k++] = a[j++]; m.movimentos++; }
        for (int p = lo; p <= hi; p++) { a[p] = aux[p]; m.movimentos++; }
    }

    static boolean estaOrdenado(int[] a) { for (int i=1;i<a.length;i++) if(a[i-1]>a[i]) return false; return true; }

    static int[] randomData(int n) {
        int[] a = new int[n];
        for (int i=0;i<n;i++) a[i]=i;
        for (int i=n-1;i>0;i--) { int j=ALEATORIO.nextInt(i+1); int x=a[i];a[i]=a[j];a[j]=x; }
        return a;
    }
    static int[] orderedData(int n) { int[] a=new int[n]; for(int i=0;i<n;i++)a[i]=i; return a; }
    static int[] copy(int[] a) { return Arrays.copyOf(a,a.length); }

    interface Sorter { Metricas run(int[] a); }

    static void benchSearch(String type, int[] base, int alvo, PrintWriter out) {
        double sumLinear=0,sumBinary=0; long lc=0,bc=0;
        for(int r=0;r<EXECUCOES;r++) {
            Metricas l=buscaLinear(base,alvo); Metricas b=buscaBinaria(base,alvo);
            if(r>0){sumLinear+=l.nanos/1e6; sumBinary+=b.nanos/1e6; lc+=l.comparacoes; bc+=b.comparacoes;}
        }
        out.printf(Locale.US,"%s,%d,%s,Busca Linear,%.6f,%d,0,\n",type,base.length,"",sumLinear/3,lc/3); out.flush();
        int[] sorted = copy(base); Arrays.sort(sorted);
        sumBinary=0; bc=0;
        for(int r=0;r<EXECUCOES;r++){Metricas b=buscaBinaria(sorted,alvo); if(r>0){sumBinary+=b.nanos/1e6;bc+=b.comparacoes;}}
        out.printf(Locale.US,"%s,%d,%s,Busca Binaria,%.6f,%d,0,\n",type,base.length,"",sumBinary/3,bc/3); out.flush();
    }

    static void benchSort(String type, int[] base, PrintWriter out, String alg, Sorter sorter) {
        double time=0; long comps=0,moves=0;
        for(int r=0;r<EXECUCOES;r++){
            int[] a=copy(base);
            Metricas m=sorter.run(a);
            if(!m.correto) throw new IllegalStateException("Ordenacao incorreta: "+alg);
            if(r>0){time+=m.nanos/1e6;comps+=m.comparacoes;moves+=m.movimentos;}
        }
        out.printf(Locale.US,"%s,%d,%s,%s,%.6f,%d,%d,\n",type,base.length,"",alg,time/3,comps/3,moves/3); out.flush();
    }

    public static void main(String[] args) throws Exception {
        String outFile = args.length>0 ? args[0] : "resultados.csv";
        try(PrintWriter out=new PrintWriter(new BufferedWriter(new FileWriter(outFile)))){
            out.println("tipo,n,estrutura,algoritmo,tempo_ms_medio,comparacoes_medias,movimentos_medios,observacao");
            for(int n:TAMANHOS){
                System.out.println("n="+n+" random");
                int[] random=randomData(n);
                benchSearch("Aleatorio",random,n-1,out);
                benchSort("Aleatorio",random,out,"Selection Sort",CorridaDaEficiencia::selectionSort);
                benchSort("Aleatorio",random,out,"Merge Sort",CorridaDaEficiencia::mergeSort);
                System.out.println("n="+n+" ordered");
                int[] ordered=orderedData(n);
                benchSearch("Ordenado",ordered,n-1,out);
                benchSort("Ordenado",ordered,out,"Selection Sort",CorridaDaEficiencia::selectionSort);
                benchSort("Ordenado",ordered,out,"Merge Sort",CorridaDaEficiencia::mergeSort);
            }
        }
        System.out.println("CSV: "+outFile);
    }
}
