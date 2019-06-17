package no.statkart.launcher.client;

class Credentials {

   private final String server;
   private final String user;
   private final String pass;

   Credentials(String server, String user, String pass) {
      this.server = server;
      this.user = user;
      this.pass = pass;
   }

   String getServer() {
      return server;
   }

   String getUser() {
      return user;
   }

   String getPass() {
      return pass;
   }

}
