FROM redis@sha256:43dbbe6f633572d66e84e7162245d8d70a18f00b819d6c0bf98ca02109f4788c
ADD create-cluster.sh /redis/create-cluster.sh
RUN chmod 755 /redis/create-cluster.sh
CMD ["/redis/create-cluster.sh"]
