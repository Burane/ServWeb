<!DOCTYPE html>
<html lang="fr">

<head>
  <meta charset="utf-8">
  <title> Test de PHP </title>
</head>

<body>
  <?php
  //phpinfo();
  for ($i = 0; $i < 7; ++$i) {
    echo "Voici le nombre $i.<br>\n";
  };
  echo "<hr/>Variables POST : <br/>\n";
  var_dump($_POST);
  echo "<hr/>Variables GET : <br/>\n";
  var_dump($_GET);
  //echo "<hr/>Variables ENV : <br/>\n";
  //var_dump($_ENV);
  //echo "<hr/>Fonction getenv() : <br/>\n";
  //var_dump(getenv());
  ?>
  <hr />
  <form method="post" action="#">
    <label for="champ1"> À passer au script PHP en POST :</label><br />
    <input name="champPost" value="données POST" id="champ1"><br />
    <button type="submit">Envoyer</button>
  </form>
  <hr />
  <form method="get" action="#">
    <label for="champ2"> À passer au script PHP en GET :</label><br />
    <input name="champGet" value="données GET" id="champ2"><br />
    <button type="submit">Envoyer</button>
  </form>
</body>

</html>