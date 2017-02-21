<!DOCTYPE html>
<html lang="en" ng-app="dev">
  <head>
    <meta charset="utf-8">
    <title>AAC Developers</title>
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="description" content="">
    <meta name="author" content="">

    <!-- Le styles -->
    <link href="css/bootstrap.min.css" rel="stylesheet">
    <style type="text/css">
      body {
        padding-top: 60px;
        padding-bottom: 40px;
      }
      .sidebar-nav {
        padding: 9px 0;
      }
      table.idps, table.idps th, table.idps td {  
        border: none;  
      }
    </style>
    <link href="css/bootstrap.min.css" rel="stylesheet">

    <script src="https://ajax.googleapis.com/ajax/libs/angularjs/1.2.10/angular.min.js"></script>
    <script src="https://ajax.googleapis.com/ajax/libs/angularjs/1.2.10/angular-resource.min.js"></script>
    <script src="lib/jquery.js"></script>
    <script src="lib/bootstrap.min.js"></script>
    <script src="js/services.js"></script>
  </head>

  <body ng-controller="MainController">
    <div class="navbar navbar-fixed-top navbar-inverse" role="navigation">
      <div class="container">
        <div class="navbar-header">
          <button type="button" class="navbar-toggle" data-toggle="collapse" data-target=".navbar-collapse">
            <span class="sr-only">Toggle navigation</span>
            <span class="icon-bar"></span>
            <span class="icon-bar"></span>
            <span class="icon-bar"></span>
          </button>
        </div>
        <div class="collapse navbar-collapse">	        
          <ul class="nav navbar-nav">
	          <li class="{{activeView('apps')}}"><a href="#" ng-click="currentView='apps'">API Clients</a></li>
            <li class="{{activeView('services')}}"><a href="#" ng-click="currentView='services'">My Services</a></li>
	          <li class="{{activeView('profile')}}"><a href="#" ng-click="currentView='profile'">Profile</a></li>
	        </ul>
	        <ul class="nav navbar-nav pull-right">
	          <li id="fat-menu" class="dropdown">
	            <a href="#" id="drop3" role="button" class="dropdown-toggle" data-toggle="dropdown"><%=request.getAttribute("username") %> <b class="caret"></b></a>
	            <ul class="dropdown-menu" role="menu" aria-labelledby="drop3">
	              <li role="presentation"><a role="menuitem" href="#" ng-click="signOut()">Sign out</a></li>
	            </ul>
	          </li>
	        </ul>
	      </div>  
      </div>
    </div>
    <div class="container">
	    <div ng-include="'./html/'+currentView+'.html'"></div>
	    <hr>
	    <footer>
	      <p>&copy; Smart Community Lab 2015</p>
	    </footer>
    </div>
  </body>
</html>
