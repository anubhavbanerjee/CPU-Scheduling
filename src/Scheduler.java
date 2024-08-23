import java.util.*;

public class Scheduler {

    private static final String LOG_OPERATION = "log";
    private static final String SHOW_STATISTICS = "stats";
    private static final String[] ALGORITHMS = {"", "FCFS", "RR-", "SPN", "SRT"};

    private List<Tuple> processes = new ArrayList<>();
    private int lastInstant;
    private int processCount;
    private char[][] timeline;
    private int[] finishTime;
    private int[] turnAroundTime;
    private double[] normTurn;

    private boolean sortByServiceTime(Tuple a, Tuple b) {
        return a.serviceTime < b.serviceTime;
    }

    private boolean sortByArrivalTime(Tuple a, Tuple b) {
        return a.arrivalTime < b.arrivalTime;
    }

    private void clearTimeline() {
        for (int i = 0; i < lastInstant; i++)
            for (int j = 0; j < processCount; j++)
                timeline[i][j] = ' ';
    }

    private void fillInWaitTime() {
        for (int i = 0; i < processCount; i++) {
            int arrivalTime = processes.get(i).arrivalTime;
            for (int k = arrivalTime; k < finishTime[i]; k++) {
                if (timeline[k][i] != '*')
                    timeline[k][i] = '.';
            }
        }
    }

    private void firstComeFirstServe() {
        int time = processes.get(0).arrivalTime;
        for (int i = 0; i < processCount; i++) {
            int arrivalTime = processes.get(i).arrivalTime;
            int serviceTime = processes.get(i).serviceTime;

            finishTime[i] = time + serviceTime;
            turnAroundTime[i] = finishTime[i] - arrivalTime;
            normTurn[i] = (double) turnAroundTime[i] / serviceTime;

            for (int j = time; j < finishTime[i]; j++)
                timeline[j][i] = '*';
            for (int j = arrivalTime; j < time; j++)
                timeline[j][i] = '.';
            time += serviceTime;
        }
    }

    private void roundRobin(int originalQuantum) {
        Queue<Pair<Integer, Integer>> queue = new LinkedList<>();
        int j = 0;
        if (processes.get(j).arrivalTime == 0) {
            queue.add(new Pair<>(j, processes.get(j).serviceTime));
            j++;
        }
        int currentQuantum = originalQuantum;
        for (int time = 0; time < lastInstant; time++) {
            if (!queue.isEmpty()) {
                int processIndex = queue.peek().first;
                queue.peek().second -= 1;
                int remainingServiceTime = queue.peek().second;
                int arrivalTime = processes.get(processIndex).arrivalTime;
                int serviceTime = processes.get(processIndex).serviceTime;
                currentQuantum--;
                timeline[time][processIndex] = '*';
                while (j < processCount && processes.get(j).arrivalTime == time + 1) {
                    queue.add(new Pair<>(j, processes.get(j).serviceTime));
                    j++;
                }

                if (currentQuantum == 0 && remainingServiceTime == 0) {
                    finishTime[processIndex] = time + 1;
                    turnAroundTime[processIndex] = finishTime[processIndex] - arrivalTime;
                    normTurn[processIndex] = (double) turnAroundTime[processIndex] / serviceTime;
                    currentQuantum = originalQuantum;
                    queue.poll();
                } else if (currentQuantum == 0 && remainingServiceTime != 0) {
                    queue.poll();
                    queue.add(new Pair<>(processIndex, remainingServiceTime));
                    currentQuantum = originalQuantum;
                } else if (currentQuantum != 0 && remainingServiceTime == 0) {
                    finishTime[processIndex] = time + 1;
                    turnAroundTime[processIndex] = finishTime[processIndex] - arrivalTime;
                    normTurn[processIndex] = (double) turnAroundTime[processIndex] / serviceTime;
                    queue.poll();
                    currentQuantum = originalQuantum;
                }
            }
            while (j < processCount && processes.get(j).arrivalTime == time + 1) {
                queue.add(new Pair<>(j, processes.get(j).serviceTime));
                j++;
            }
        }
        fillInWaitTime();
    }

    private void shortestProcessNext() {
        PriorityQueue<Pair<Integer, Integer>> pq = new PriorityQueue<>(Comparator.comparingInt(a -> a.first));
        int j = 0;
        for (int i = 0; i < lastInstant; i++) {
            while (j < processCount && processes.get(j).arrivalTime <= i) {
                pq.add(new Pair<>(processes.get(j).serviceTime, j));
                j++;
            }
            if (!pq.isEmpty()) {
                int processIndex = pq.poll().second;
                int arrivalTime = processes.get(processIndex).arrivalTime;
                int serviceTime = processes.get(processIndex).serviceTime;

                int temp = arrivalTime;
                for (; temp < i; temp++)
                    timeline[temp][processIndex] = '.';

                temp = i;
                for (; temp < i + serviceTime; temp++)
                    timeline[temp][processIndex] = '*';

                finishTime[processIndex] = i + serviceTime;
                turnAroundTime[processIndex] = finishTime[processIndex] - arrivalTime;
                normTurn[processIndex] = (double) turnAroundTime[processIndex] / serviceTime;
                i = temp - 1;
            }
        }
    }

    private void shortestRemainingTime() {
        PriorityQueue<Pair<Integer, Integer>> pq = new PriorityQueue<>(Comparator.comparingInt(a -> a.first));
        int j = 0;
        for (int i = 0; i < lastInstant; i++) {
            while (j < processCount && processes.get(j).arrivalTime == i) {
                pq.add(new Pair<>(processes.get(j).serviceTime, j));
                j++;
            }
            if (!pq.isEmpty()) {
                int processIndex = pq.poll().second;
                int remainingTime = pq.poll().first;
                int serviceTime = processes.get(processIndex).serviceTime;
                int arrivalTime = processes.get(processIndex).arrivalTime;
                timeline[i][processIndex] = '*';

                if (remainingTime == 1) {
                    finishTime[processIndex] = i + 1;
                    turnAroundTime[processIndex] = finishTime[processIndex] - arrivalTime;
                    normTurn[processIndex] = (double) turnAroundTime[processIndex] / serviceTime;
                } else {
                    pq.add(new Pair<>(remainingTime - 1, processIndex));
                }
            }
        }
        fillInWaitTime();
    }

    private void printAlgorithm(int algorithmIndex) {
        System.out.println(ALGORITHMS[algorithmIndex]);
    }

    private void printProcesses() {
        System.out.print("Process    ");
        for (int i = 0; i < processCount; i++)
            System.out.print("|  " + processes.get(i).processName + "  ");
        System.out.println("|");
    }

    private void printArrivalTime() {
        System.out.print("Arrival    ");
        for (int i = 0; i < processCount; i++)
            System.out.printf("|%3d  ", processes.get(i).arrivalTime);
        System.out.println("|");
    }

    private void printServiceTime() {
        System.out.print("Service    |");
        for (int i = 0; i < processCount; i++) {
            System.out.printf("%3d  |", processes.get(i).serviceTime);
        }
        System.out.println(" Mean|");
    }

    private void printFinishTime() {
        System.out.print("Finish     ");
        for (int i = 0; i < processCount; i++)
            System.out.printf("|%3d  ", finishTime[i]);
        System.out.println("|-----|");
    }

    private void printTurnAroundTime() {
        System.out.print("Turnaround |");
        int sum = 0;
        for (int i = 0; i < processCount; i++) {
            System.out.printf("%3d  |", turnAroundTime[i]);
            sum += turnAroundTime[i];
        }
        System.out.printf("%2.2f|\n", (double) sum / turnAroundTime.length);
    }

    private void printNormTurn() {
        System.out.print("NormTurn   |");
        double sum = 0;
        for (int i = 0; i < processCount; i++) {
            System.out.printf("%2.2f|", normTurn[i]);
            sum += normTurn[i];
        }
        System.out.printf("%2.2f|\n", (double) sum / normTurn.length);
    }

    private void printStats(int algorithmIndex) {
        printAlgorithm(algorithmIndex);
        printProcesses();
        printArrivalTime();
        printServiceTime();
        printFinishTime();
        printTurnAroundTime();
        printNormTurn();
    }

    private void printTimeline(int algorithmIndex) {
        printAlgorithm(algorithmIndex);
        for (int i = 0; i < processCount; i++) {
            System.out.print(processes.get(i).processName + "       |");
            for (int j = 0; j < lastInstant; j++)
                System.out.print(" " + timeline[j][i] + " |");
            System.out.println();
        }
        System.out.print("------------------------------------------------");
        for (int i = 0; i < lastInstant; i++) System.out.print("--");
        System.out.println();
    }

    public Scheduler() {
        Scanner sc = new Scanner(System.in);
        System.out.println("Select an algorithm:");
        System.out.println("1 - First Come First Serve (FCFS)");
        System.out.println("2 - Round Robin (RR)");
        System.out.println("3 - Shortest Process Next (SPN)");
        System.out.println("4 - Shortest Remaining Time (SRT)");

        String input = sc.nextLine();
        String[] splitInput = input.split(" ");
        String algorithmChoice = splitInput[0];
        int algorithmIndex = Integer.parseInt(algorithmChoice);
        boolean isRoundRobin = algorithmIndex == 2;
        int quantum = isRoundRobin ? Integer.parseInt(splitInput[splitInput.length - 3]) : 0;
        boolean log = input.contains(LOG_OPERATION);
        boolean stats = input.contains(SHOW_STATISTICS);

        for (int i = 1; i < splitInput.length - (isRoundRobin ? 3 : 2); i++) {
            String[] processDetails = splitInput[i].split(",");
            String processName = processDetails[0];
            int arrivalTime = Integer.parseInt(processDetails[1]);
            int serviceTime = Integer.parseInt(processDetails[2]);
            processes.add(new Tuple(processName, arrivalTime, serviceTime));
        }

        if (processes.isEmpty()) {
            System.out.println("No processes provided. Exiting.");
            return;
        }

        processes.sort(Comparator.comparingInt(a -> a.arrivalTime));
        processCount = processes.size();
        lastInstant = processes.get(processCount - 1).arrivalTime + processes.stream().mapToInt(a -> a.serviceTime).sum();

        timeline = new char[lastInstant][processCount];
        finishTime = new int[processCount];
        turnAroundTime = new int[processCount];
        normTurn = new double[processCount];

        clearTimeline();

        switch (algorithmIndex) {
            case 1 -> firstComeFirstServe();
            case 2 -> roundRobin(quantum);
            case 3 -> shortestProcessNext();
            case 4 -> shortestRemainingTime();
        }

        if (log) printTimeline(algorithmIndex);
        if (stats) printStats(algorithmIndex);
    }

    public static void main(String[] args) {
        new Scheduler();
    }

    private static class Tuple {
        String processName;
        int arrivalTime;
        int serviceTime;

        public Tuple(String processName, int arrivalTime, int serviceTime) {
            this.processName = processName;
            this.arrivalTime = arrivalTime;
            this.serviceTime = serviceTime;
        }
    }

    private static class Pair<A, B> {
        A first;
        B second;

        public Pair(A first, B second) {
            this.first = first;
            this.second = second;
        }
    }
}

