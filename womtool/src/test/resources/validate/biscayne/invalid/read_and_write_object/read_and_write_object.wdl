version development

workflow read_and_write_object {
  File foo = write_object( { "a": "b" } )
  Map[String, String] foo_back = read_object( { "a": "b" } )

  output {
    File foo_out = foo
  }
}
