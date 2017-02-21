<!DOCTYPE html>
<html lang="en" ng-app="admin">
  <head>
    <meta charset="utf-8">
    <title>AAC Developers</title>
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="description" content="">
    <meta name="author" content="">

    <!-- Le styles -->
    <link href="css/bootstrap.min.css" rel="stylesheet">
    <link href="css/bs-ext.css" rel="stylesheet">
    <style type="text/css">
      body {
        padding-top: 60px;
        padding-bottom: 40px;
      }
      .sidebar-nav {
        padding: 9px 0;
      }
    </style>
    <link href="css/bootstrap-responsive.css" rel="stylesheet">

    <script src="https://ajax.googleapis.com/ajax/libs/angularjs/1.0.7/angular.min.js"></script>
    <script src="https://ajax.googleapis.com/ajax/libs/angularjs/1.0.7/angular-resource.min.js"></script>
    <script src="https://ajax.googleapis.com/ajax/libs/angularjs/1.0.7/angular-cookies.min.js"></script>
    <script src="lib/jquery.js"></script>
    <script src="lib/bootstrap.min.js"></script>
    <script src="js/admin.js"></script>
  </head>

  <body ng-controller="AdminController">
    <div class="navbar navbar-fixed-top navbar-inverse ">
      <div class="navbar-inner">
        <div class="container" >
<!--           <ul class="nav" role="navigation"> -->
<!--             <li class="active"><a href="#" ng-click="currentView='approvals'">App approvals</a></li> -->
<!--           </ul> -->
          <ul class="nav pull-right">
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
      <div class="row">
        <div class="span2">
			    <div class="well sidebar-nav">
			      <ul class="nav nav-list">
			        <li><a href="#" ng-click="currentView='approvals';title='App Approvals'">App approvals</a></li>
              <li><a href="#" ng-click="currentView='idps';title='IdP Approvals'">IdP approvals</a></li>
			      </ul>
			    </div>
		    <!--/.well -->
		    </div>
			  <div class="span9 well">
			    <div class="alert alert-error" ng-show="error != ''">{{error}}</div>
			    <div class="alert alert-success" ng-show="info != ''">{{info}}</div>
			    <div class="row">
			      <div class="span7">
			          <strong>{{title}}</strong>
			      </div>
			    </div>
	        <div ng-include="'./html/'+currentView+'.html'"></div>
	      </div>
      </div>
	    <hr>
	    <footer>
	      <p>&copy; Smart Community Lab 2015</p>
	    </footer>
    </div>
  </body>
</html>
