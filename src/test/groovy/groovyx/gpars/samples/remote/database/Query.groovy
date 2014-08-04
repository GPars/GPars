package groovyx.gpars.samples.remote.database

def reader = new BufferedReader(new InputStreamReader(System.in))

while (true) {
    println "query>"
    def query = reader.readLine()
    println query
}
