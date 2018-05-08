#!/bin/bash
#set -x
export LANG=en_US.utf8

basepath=$(cd "$(dirname "$0")"; pwd)

cd ${basepath}

logdir='/data/applogs/linkluence-message-task/logs'
now=$(date '+%Y%m%d')
logfile=stdout-${now}.log
JAVA_BIN=/usr/local/jdk1.8.0_141/bin

function start() {
    if [ ! -d $logdir ] ; then 
	mkdir -p ${logdir}
    fi
    
    pid=`${JAVA_BIN}/jps|awk '/linkluence-message-task/{print $1}'`

    if [ -z ${pid} ] ;then
	echo "starting ...................."
    	${JAVA_BIN}/java -jar -Xmx512m ../linkluence-message-task.jar >> ${logdir}/${logfile} 2>&1 &
	RETVAL=$?
	if [ $RETVAL -eq 0 ]; then
	    echo "start ok"
	else
	    echo "start failed"
	fi
    else
	echo "linkluence-message-task is running"
    fi
}

function stop()  {
    
    pid=`${JAVA_BIN}/jps|awk '/linkluence-message-task/{print $1}'`
    
    if [ -z ${pid} ] ; then
	echo "linkluence-message-task is already stoped"
    else
	echo "stoping ...."
	kill -9 ${pid}
	RETVAL=$?
        if [ $RETVAL -eq 0 ]; then
            echo "stop ok"
        else
            echo "stop failed"
        fi
    fi
}

case "$1" in
  start)
        start
        ;;
  stop)
        stop
        ;;
  restart)
        stop
        start
        ;;
  *)
        echo $"Usage: $0 {start|stop|restart}"
        exit 1
esac

